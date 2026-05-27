package com.MentalHealth.ApnaBuddyBackend.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class GameDtos {

    public record SkribbleRequest(
            @NotBlank String prompt,
            @NotBlank(message = "Drawing data cannot be empty") String drawingDataBase64
    ) {}

    public record SkribbleResponse(
            Long id,
            String prompt,
            String drawingDataBase64,
            Instant createdAt
    ) {}

    public record PromptResponse(
            String prompt
    ) {}
}