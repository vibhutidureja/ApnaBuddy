package com.MentalHealth.ApnaBuddyBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OnboardingRequest(
        @NotBlank String nickname,
        String ageGroup,
        String gender,
        String country,
        @NotBlank String occupation,
        String occupationDetails,
        @NotBlank String feelingToday,
        List<String> primaryConcerns,
        @NotNull Integer overwhelmedScale,
        @NotBlank String overwhelmedFrequency,
        @NotNull Boolean safetyRisk,
        List<String> copingMechanisms,
        String sleepQuality,
        List<String> goals,
        @NotBlank String chatPreference
) {}