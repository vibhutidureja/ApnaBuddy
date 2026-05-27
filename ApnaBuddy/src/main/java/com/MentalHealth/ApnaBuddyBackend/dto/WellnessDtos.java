package com.MentalHealth.ApnaBuddyBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class WellnessDtos {

    public record JournalRequest(
            @NotBlank String journalType, // e.g., "FREEFORM" or "PROMPTED"
            @NotBlank String content,
            String promptUsed // Included for AI-prompted journals
    ) {}

    public record JournalResponse(
            Long id,
            String journalType,
            String content,
            String promptUsed,
            String sentiment,
            Instant createdAt
    ) {}

    public record ActivityLogRequest(
            @NotBlank String activityType,
            @NotNull Integer durationSeconds,
            @NotNull Integer moodBefore,
            @NotNull Integer moodAfter
    ) {}

    // --- NEW: Fire Game Request ---
    public record FireGameRequest(
            @NotBlank String thoughtToBurn,
            @NotNull Integer moodBefore,
            @NotNull Integer moodAfter
    ) {}
}