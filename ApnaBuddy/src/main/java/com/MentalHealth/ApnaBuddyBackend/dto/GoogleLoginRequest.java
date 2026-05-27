package com.MentalHealth.ApnaBuddyBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google ID Token is required")
        String idToken
) {}
