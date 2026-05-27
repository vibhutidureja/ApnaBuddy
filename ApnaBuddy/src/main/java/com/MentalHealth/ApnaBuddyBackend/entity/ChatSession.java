package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, unique = true)
    String springAiConversationId;

    String sessionTitle;

    @Column(columnDefinition = "text")
    String activeStrategyContext;

    // --- 1. SUICIDE TRACKING (Existing) ---
    @Builder.Default
    @Column(nullable = false)
    Integer interactionCount = 0;

    @Builder.Default
    @Column(nullable = false)
    Double cumulativeSuicideScore = 0.0;

    @Builder.Default
    @Column(nullable = false)
    Double averageSuicideScore = 0.0;

    // --- 2. DEPRESSION TRACKING (New: For Line Charts) ---
    @Builder.Default
    @Column(nullable = false)
    Double cumulativeDepressionScore = 0.0;

    @Builder.Default
    @Column(nullable = false)
    Double averageDepressionScore = 0.0;

    // --- 3. PEAK & DOMINANT METRICS (New: For Pie Charts & Insights) ---
    @Builder.Default
    String maxRiskLevel = "Low"; // Captures if the session ever hit "CRITICAL" or "High"

    String overallSentiment;    // Stores dominant sentiment (Positive/Negative/Neutral)
    String overallEmotion;      // Stores the most frequent emotion detected in this session

    @Column(columnDefinition = "TEXT")
    String sessionSummary;      // For AI-generated "Executive Summaries" in reports

    // --- 4. INFRASTRUCTURE (Existing) ---
    @Builder.Default
    @Column(nullable = false)
    Integer totalTokensUsed = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    Instant deletedAt;
}