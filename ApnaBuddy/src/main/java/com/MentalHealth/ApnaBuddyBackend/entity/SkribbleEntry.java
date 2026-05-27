package com.MentalHealth.ApnaBuddyBackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "skribble_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkribbleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, columnDefinition = "text")
    String prompt; // The dynamic question they were asked to draw

    @Column(nullable = false, columnDefinition = "text")
    String drawingDataBase64; // The actual drawing saved as a Base64 image string

    @CreationTimestamp
    @Column(updatable = false)
    Instant createdAt;
}