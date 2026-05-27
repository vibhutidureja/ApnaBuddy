package com.MentalHealth.ApnaBuddyBackend.dto;

public record OnboardingProfileResponse(
        String nickname,
        String ageGroup,
        String gender,
        String occupation,
        String goals,
        String copingMechanisms,
        String primaryConcerns,
        String feelingToday
) {}