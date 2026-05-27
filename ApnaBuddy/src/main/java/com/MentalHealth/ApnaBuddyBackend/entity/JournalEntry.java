package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String journalType; // "GRATITUDE", "VENT", "DAILY_REFLECTION"

    @Column(columnDefinition = "text", nullable = false)
    String content;

    String sentiment;
    Double emotionScore;
    @Column(columnDefinition = "text")
    String promptUsed;
    @CreationTimestamp
    @Column(updatable = false)
    Instant createdAt;
}