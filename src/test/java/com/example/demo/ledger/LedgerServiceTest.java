package com.example.demo.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.ledger.LedgerService.Line;
import com.example.demo.support.AbstractIntegrationTest;

class LedgerServiceTest extends AbstractIntegrationTest {

    @Autowired
    LedgerService ledger;
    @Autowired
    JournalEntryRepository entries;
    @Autowired
    TransactionTemplate tx;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void balancedEntryIsPostedAndMovesAccountBalances() {
        long receivableBefore = ledger.balance(LedgerAccounts.MOMO_RECEIVABLE);
        long revenueBefore = ledger.balance(LedgerAccounts.SALES_REVENUE);

        tx.executeWithoutResult(s -> ledger.post("TEST", uniqueId(), "sale", List.of(
                Line.debit(LedgerAccounts.MOMO_RECEIVABLE, 250_000),
                Line.credit(LedgerAccounts.SALES_REVENUE, 250_000))));

        assertThat(ledger.balance(LedgerAccounts.MOMO_RECEIVABLE)).isEqualTo(receivableBefore + 250_000);
        // Revenue is a credit-normal account: its net debit goes down.
        assertThat(ledger.balance(LedgerAccounts.SALES_REVENUE)).isEqualTo(revenueBefore - 250_000);
    }

    @Test
    void postingTheSameSourceTwiceCreatesOneEntry() {
        String sourceId = uniqueId();
        List<Line> lines = List.of(Line.debit(LedgerAccounts.MOMO_RECEIVABLE, 1_000),
                Line.credit(LedgerAccounts.SALES_REVENUE, 1_000));

        Long first = tx.execute(s -> ledger.post("TEST", sourceId, "x", lines).getId());
        Long second = tx.execute(s -> ledger.post("TEST", sourceId, "x", lines).getId());

        assertThat(second).isEqualTo(first);
        assertThat(entries.findBySourceTypeAndSourceId("TEST", sourceId)).isPresent();
    }

    @Test
    void unbalancedEntryIsRejectedByTheService() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> ledger.post("TEST", uniqueId(), "bad", List.of(
                Line.debit(LedgerAccounts.MOMO_RECEIVABLE, 1_000),
                Line.credit(LedgerAccounts.SALES_REVENUE, 999)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void postingOutsideATransactionIsRefused() {
        assertThatThrownBy(() -> ledger.post("TEST", uniqueId(), "x", List.of(
                Line.debit(LedgerAccounts.MOMO_RECEIVABLE, 1_000),
                Line.credit(LedgerAccounts.SALES_REVENUE, 1_000))))
                .hasMessageContaining("mandatory");
    }

    @Test
    void databaseRejectsAnUnbalancedEntryEvenIfApplicationCodeIsBypassed() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            Long entryId = jdbc.queryForObject("""
                    insert into journal_entries (source_type, source_id, description)
                    values ('RAW', ?, 'bypass') returning id
                    """, Long.class, uniqueId());
            jdbc.update("insert into journal_lines (entry_id, account_code, direction, amount) values (?, '1130', 'DEBIT', 500)",
                    entryId);
            jdbc.update("insert into journal_lines (entry_id, account_code, direction, amount) values (?, '5110', 'CREDIT', 400)",
                    entryId);
        })).hasStackTraceContaining("is not balanced");
    }

    @Test
    void databaseForbidsEditingOrDeletingPostedLines() {
        String sourceId = uniqueId();
        tx.executeWithoutResult(s -> ledger.post("TEST", sourceId, "x", List.of(
                Line.debit(LedgerAccounts.MOMO_RECEIVABLE, 7_000),
                Line.credit(LedgerAccounts.SALES_REVENUE, 7_000))));
        Long entryId = entries.findBySourceTypeAndSourceId("TEST", sourceId).orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update("update journal_lines set amount = 1 where entry_id = ?", entryId))
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from journal_lines where entry_id = ?", entryId))
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from journal_entries where id = ?", entryId))
                .hasStackTraceContaining("append-only");
    }

    @Test
    void wholeLedgerAlwaysBalances() {
        assertThat(entries.total(JournalLine.Direction.DEBIT)).isEqualTo(entries.total(JournalLine.Direction.CREDIT));
    }

    private static String uniqueId() {
        return UUID.randomUUID().toString();
    }
}
