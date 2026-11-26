package com.example.demo.reconciliation;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.payment.PaymentAttempt;
import com.example.demo.payment.PaymentAttemptRepository;
import com.example.demo.payment.PaymentStatusQuerier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Compares what VESTRA believes against MoMo and against its own ledger, and records every difference as an item
 * for a human to resolve. Items are unique per (type, reference), so running often never duplicates them.
 *
 * <ol>
 *   <li>Lost IPN: attempts still pending after {@link #STALE_AFTER} are queried at MoMo and the answer applied.</li>
 *   <li>PAID payment without its ledger entry, or with a different amount.</li>
 *   <li>Payment status and order payment status disagree.</li>
 *   <li>Anomalies recorded while handling results (amount mismatch, paid twice, paid after close, unknown order).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final PaymentAttemptRepository attemptRepository;
    private final PaymentStatusQuerier querier;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public record RunSummary(Long runId, int queriedAttempts, int newItems) {
    }

    public RunSummary run(String trigger) {
        Long runId = jdbc.queryForObject(
                "insert into reconciliation_runs (trigger) values (?) returning id", Long.class, trigger);

        List<PaymentAttempt> stale = attemptRepository.findStalePending(LocalDateTime.now(clock).minus(STALE_AFTER));
        for (PaymentAttempt attempt : stale) {
            querier.queryAndApply(attempt);
        }

        int newItems = 0;
        newItems += jdbc.update("""
                insert into reconciliation_items (run_id, type, reference, payment_id, order_id, details)
                select ?, 'LEDGER_MISSING', 'payment:' || p.id, p.id, p.order_id,
                       'Payment PAID (MoMo transId ' || p.provider_trans_id || ') has no ledger entry'
                from payments p
                where p.status = 'PAID'
                  and not exists (select 1 from journal_entries e
                                  where e.source_type = 'MOMO_PAYMENT'
                                    and e.source_id = 'trans:' || p.provider_trans_id)
                on conflict (type, reference) do nothing
                """, runId);
        newItems += jdbc.update("""
                insert into reconciliation_items (run_id, type, reference, payment_id, order_id, details)
                select ?, 'LEDGER_AMOUNT_MISMATCH', 'payment:' || p.id, p.id, p.order_id,
                       'Payment amount ' || p.amount || ' but ledger debited ' || l.amount
                from payments p
                join journal_entries e on e.source_type = 'MOMO_PAYMENT' and e.source_id = 'trans:' || p.provider_trans_id
                join journal_lines l on l.entry_id = e.id and l.direction = 'DEBIT' and l.account_code = '1130'
                where p.status = 'PAID' and l.amount <> p.amount
                on conflict (type, reference) do nothing
                """, runId);
        newItems += jdbc.update("""
                insert into reconciliation_items (run_id, type, reference, payment_id, order_id, details)
                select ?, 'STATUS_MISMATCH', 'payment:' || p.id || ':' || p.status, p.id, o.id,
                       'payments.status=' || p.status || ' but orders.payment_status=' || o.payment_status
                from payments p join orders o on o.id = p.order_id
                where o.payment_status <> p.status
                on conflict (type, reference) do nothing
                """, runId);
        newItems += jdbc.update("""
                insert into reconciliation_items (run_id, type, reference, payment_id, order_id, details)
                select ?, ev.outcome, 'event:' || ev.id, ev.payment_id, p.order_id,
                       'MoMo ' || ev.source || ' for ' || coalesce(ev.provider_order_id, '?')
                       || ', amount ' || coalesce(ev.amount::text, '?')
                       || ', transId ' || coalesce(ev.trans_id::text, '?')
                from payment_events ev left join payments p on p.id = ev.payment_id
                where ev.outcome in ('AMOUNT_MISMATCH', 'UNKNOWN_ORDER', 'DUPLICATE_PAYMENT', 'PAID_AFTER_CLOSE')
                on conflict (type, reference) do nothing
                """, runId);

        jdbc.update("update reconciliation_runs set finished_at = now(), queried_attempts = ?, new_items = ? where id = ?",
                stale.size(), newItems, runId);
        if (newItems > 0) {
            log.warn("Reconciliation run {} found {} new discrepancies", runId, newItems);
        }
        return new RunSummary(runId, stale.size(), newItems);
    }

    public List<Map<String, Object>> items(String status) {
        return jdbc.queryForList("""
                select id, run_id as "runId", type, reference, payment_id as "paymentId", order_id as "orderId",
                       details, status, resolution_note as "resolutionNote", created_at as "createdAt",
                       resolved_at as "resolvedAt"
                from reconciliation_items where status = ? order by id desc limit 200
                """, status);
    }

    /** Returns false if the item does not exist or is already resolved. */
    public boolean resolve(Long itemId, String note) {
        return jdbc.update("""
                update reconciliation_items set status = 'RESOLVED', resolution_note = ?, resolved_at = now()
                where id = ? and status = 'OPEN'
                """, note, itemId) == 1;
    }
}
