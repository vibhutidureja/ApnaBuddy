package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_onboarding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    String nickname;
    String ageGroup;
    String gender;
    String country;
    String occupation;
    String occupationDetails;
    String feelingToday;

    @Column(columnDefinition = "text")
    String primaryConcerns;

    Integer overwhelmedScale;
    String overwhelmedFrequency;
    Boolean safetyRisk;

    @Column(columnDefinition = "text")
    String copingMechanisms;
    String sleepQuality;

    @Column(columnDefinition = "text")
    String goals;
    String chatPreference;

    @CreationTimestamp
    @Column(updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;
}