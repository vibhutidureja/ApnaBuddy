package com.MentalHealth.ApnaBuddyBackend.dto;

public record AuthResponse(
        String token, // Your backend JWT
        UserProfileResponse user
) {}