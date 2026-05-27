package com.MentalHealth.ApnaBuddyBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlAnalysisResponse(
        String translated_text,
        String risk_level,
        String sentiment,
        String emotion,
        Double emotion_score,
        Double suicidal_score,
        Double depression_score
) {}