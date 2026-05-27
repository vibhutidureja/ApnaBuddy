package com.MentalHealth.ApnaBuddyBackend.dto;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String pictureUrl,
        Boolean hasCompletedOnboarding,
        Boolean hasCompletedAssessment
) {}