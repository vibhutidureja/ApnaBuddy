package com.MentalHealth.ApnaBuddyBackend.dto;

public record Joke(
        String text,
        String category,
        Double laughScore,
        Boolean isNSFW
) {
}
