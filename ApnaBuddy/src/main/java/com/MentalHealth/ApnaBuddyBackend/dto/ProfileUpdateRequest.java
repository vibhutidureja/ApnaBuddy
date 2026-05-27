package com.MentalHealth.ApnaBuddyBackend.dto;

public record ProfileUpdateRequest(
        String nickname,
        String ageGroup,
        String gender,
        String occupation,
        String goal,
        String cope,
        String concern,
        String feelingToday
) {}