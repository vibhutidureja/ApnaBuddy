package com.MentalHealth.ApnaBuddyBackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class AssessmentDtos {

    public record AnswerSubmission(
            @NotNull Long questionId,
            @NotNull @Min(0) @Max(4) Integer answer
    ) {}

    public record AssessmentSubmitRequest(
            @NotNull @Size(min = 35, max = 35, message = "Exactly 35 answers are required")
            List<AnswerSubmission> answers
    ) {}

    public record QuestionDto(Long id, String category, String text, boolean reverse) {}

    public record CategoryReport(
            String displayName, String description, Integer score, Integer maxScore,
            Integer percent, String result, String summary, String suggestion, Boolean professionalCare
    ) {}

    public record OverallReport(
            Integer totalScore, Integer maxTotalScore, Integer wellnessScore, String result, String message
    ) {}

    public record AssessmentReport(
            Map<String, Object> meta,
            Map<String, CategoryReport> categories,
            OverallReport overall,
            List<String> areasOfConcern,
            Boolean requiresProfessionalSupport
    ) {}
}