package com.MentalHealth.ApnaBuddyBackend.entity;

import com.MentalHealth.ApnaBuddyBackend.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_session_id", nullable = false)
    ChatSession chatSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MessageRole role; // USER, ASSISTANT, SYSTEM

    @Column(columnDefinition = "text", nullable = false)
    String content;

    // --- FIX: Added @Builder.Default right here ---
    @Builder.Default
    Integer tokensUsed = 0;

    @CreationTimestamp
    @Column(updatable = false)
    Instant createdAt;
}