package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    Integer totalScore;

    @Column(nullable = false)
    Integer wellnessScore;

    @Column(nullable = false)
    String wellnessResult;

    @Column(nullable = false)
    Boolean requiresProfessionalSupport;

    @Column(columnDefinition = "text", nullable = false)
    String fullReportJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;
}