package com.example.demo.ledger;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Posts balanced, append-only journal entries. Posting is idempotent per (sourceType, sourceId): posting the same
 * business event twice returns the first entry. Balance is checked here and, independently, by a deferred database
 * trigger at commit.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    public record Line(String accountCode, JournalLine.Direction direction, long amount) {
        public static Line debit(String accountCode, long amount) {
            return new Line(accountCode, JournalLine.Direction.DEBIT, amount);
        }

        public static Line credit(String accountCode, long amount) {
            return new Line(accountCode, JournalLine.Direction.CREDIT, amount);
        }
    }

    private final JournalEntryRepository entryRepository;

    /** Must run inside the caller's transaction, so the entry commits or rolls back with the business change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry post(String sourceType, String sourceId, String description, List<Line> lines) {
        Optional<JournalEntry> existing = entryRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
        if (existing.isPresent()) {
            return existing.get();
        }
        validate(lines);

        JournalEntry entry = JournalEntry.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .description(description)
                .build();
        for (Line line : lines) {
            entry.getLines().add(JournalLine.builder()
                    .entry(entry)
                    .accountCode(line.accountCode())
                    .direction(line.direction())
                    .amount(line.amount())
                    .build());
        }
        return entryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public long balance(String accountCode) {
        return entryRepository.netDebit(accountCode);
    }

    static void validate(List<Line> lines) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two lines");
        }
        long debit = 0;
        long credit = 0;
        for (Line line : lines) {
            if (line.amount() <= 0) {
                throw new IllegalArgumentException("Journal line amounts must be positive");
            }
            if (line.direction() == JournalLine.Direction.DEBIT) {
                debit = Math.addExact(debit, line.amount());
            } else {
                credit = Math.addExact(credit, line.amount());
            }
        }
        if (debit != credit) {
            throw new IllegalArgumentException("Unbalanced entry: debit " + debit + " != credit " + credit);
        }
    }
}
