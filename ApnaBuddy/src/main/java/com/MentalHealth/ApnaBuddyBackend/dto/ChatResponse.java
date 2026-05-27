package com.MentalHealth.ApnaBuddyBackend.dto;

public record ChatResponse(
        String response,
        String actionToTrigger // <-- NEW FIELD
) {}