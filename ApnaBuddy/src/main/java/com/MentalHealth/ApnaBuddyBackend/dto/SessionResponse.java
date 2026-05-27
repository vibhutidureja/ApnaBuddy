package com.MentalHealth.ApnaBuddyBackend.dto;

import java.time.Instant;

// Make sure it is a "record", not a "class"!
public record SessionResponse(
        Long id,
        String title,
        Instant createdAt
) {}