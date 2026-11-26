package com.example.demo.ledger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A balanced journal entry. Immutable once written (the database also forbids updates and deletes). */
@Entity
@Immutable
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();
}
