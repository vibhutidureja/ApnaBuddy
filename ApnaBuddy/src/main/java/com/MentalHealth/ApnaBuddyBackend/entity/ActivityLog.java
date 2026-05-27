package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String activityType; // "BREATHING_4_7_8", "WORRY_BOX", "GROUNDING_54321"

    Integer durationSeconds;

    Integer moodBefore; // 1-10 scale
    Integer moodAfter;  // 1-10 scale

    @CreationTimestamp
    @Column(updatable = false)
    Instant completedAt;
}