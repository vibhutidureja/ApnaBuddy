package com.MentalHealth.ApnaBuddyBackend.dto;

import com.MentalHealth.ApnaBuddyBackend.enums.MessageRole;

import java.time.Instant;

public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        Instant createdAt
) {}