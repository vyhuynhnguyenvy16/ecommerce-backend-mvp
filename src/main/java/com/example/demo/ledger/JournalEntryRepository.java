package com.example.demo.ledger;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    /** Debits minus credits for one account, in VND. */
    @Query("""
            select coalesce(sum(case when l.direction = com.example.demo.ledger.JournalLine.Direction.DEBIT
                                     then l.amount else -l.amount end), 0)
            from JournalLine l where l.accountCode = :code
            """)
    long netDebit(@Param("code") String accountCode);

    @Query("""
            select coalesce(sum(l.amount), 0) from JournalLine l
            where l.direction = :direction
            """)
    long total(@Param("direction") JournalLine.Direction direction);
}
