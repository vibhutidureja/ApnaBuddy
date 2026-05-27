package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.AssessmentDtos.*;
import com.MentalHealth.ApnaBuddyBackend.entity.AssessmentRecord;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.repository.AssessmentRecordRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import com.MentalHealth.ApnaBuddyBackend.util.QuestionBank;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRecordRepository assessmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_QUESTION_SCORE = 4;
    private static final int MAX_CATEGORY_SCORE = 20;
    private static final int MAX_TOTAL_SCORE = 140;

    /**
     * Retrieves all predefined questions from the QuestionBank.
     */
    public List<QuestionDto> getAllQuestions() {
        return QuestionBank.QUESTIONS;
    }

    /**
     * Processes a user's assessment submission, calculates scores, and saves the record.
     */
    @Transactional
    public AssessmentReport submitAssessment(Long userId, AssessmentSubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Map<String, Integer> categoryRawScores = new HashMap<>();
        int totalScore = 0;
        Set<Long> seenIds = new HashSet<>();

        // Validate and score each answer
        for (AnswerSubmission ans : request.answers()) {
            if (!seenIds.add(ans.questionId())) {
                throw new IllegalArgumentException("Duplicate question ID: " + ans.questionId());
            }

            QuestionDto q = QuestionBank.QUESTION_MAP.get(ans.questionId());
            if (q == null) {
                throw new IllegalArgumentException("Invalid question ID: " + ans.questionId());
            }

            // Apply reverse scoring if necessary
            int finalScore = q.reverse() ? (MAX_QUESTION_SCORE - ans.answer()) : ans.answer();

            categoryRawScores.put(q.category(), categoryRawScores.getOrDefault(q.category(), 0) + finalScore);
            totalScore += finalScore;
        }

        // Build category-specific reports
        Map<String, CategoryReport> categoriesReport = new HashMap<>();
        List<String> areasOfConcern = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : categoryRawScores.entrySet()) {
            String category = entry.getKey();
            Integer score = entry.getValue();
            int percent = Math.round(((float) score / MAX_CATEGORY_SCORE) * 100);

            String result = getSeverityLabel(percent);
            CategoryMeta meta = getCategoryMeta(category);
            Recommendation rec = getRecommendation(result);

            categoriesReport.put(category, new CategoryReport(
                    meta.displayName(), meta.description(), score, MAX_CATEGORY_SCORE,
                    percent, result, rec.summary(), rec.suggestion(), rec.professionalCare()
            ));

            if (rec.professionalCare()) {
                areasOfConcern.add(meta.displayName());
            }
        }

        // Calculate overall wellness metrics
        int wellnessScore = Math.round(100 - (((float) totalScore / MAX_TOTAL_SCORE) * 100));
        String overallResult = getWellnessLabel(wellnessScore);
        OverallReport overall = new OverallReport(
                totalScore,
                MAX_TOTAL_SCORE,
                wellnessScore,
                overallResult,
                getWellnessMessage(overallResult)
        );

        Map<String, Object> metaMap = Map.of(
                "assessmentVersion", "1.0.0",
                "completedAt", Instant.now().toString(),
                "userId", userId.toString()
        );

        AssessmentReport report = new AssessmentReport(
                metaMap,
                categoriesReport,
                overall,
                areasOfConcern,
                !areasOfConcern.isEmpty()
        );

        saveAssessmentRecord(user, totalScore, wellnessScore, overallResult, report);

        return report;
    }

    /**
     * Persists the assessment result to the database and updates user status.
     */
    private void saveAssessmentRecord(User user, int totalScore, int wellnessScore, String overallResult, AssessmentReport report) {
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            AssessmentRecord record = AssessmentRecord.builder()
                    .user(user)
                    .totalScore(totalScore)
                    .wellnessScore(wellnessScore)
                    .wellnessResult(overallResult)
                    .requiresProfessionalSupport(report.requiresProfessionalSupport())
                    .fullReportJson(jsonReport)
                    .build();

            assessmentRepository.save(record);

            user.setHasCompletedAssessment(true);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to save assessment for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private String getSeverityLabel(int percent) {
        if (percent <= 25) return "Minimal";
        if (percent <= 50) return "Mild";
        if (percent <= 75) return "Moderate";
        return "High";
    }

    private String getWellnessLabel(int wellnessScore) {
        if (wellnessScore >= 75) return "Good";
        if (wellnessScore >= 50) return "Fair";
        if (wellnessScore >= 25) return "Low";
        return "Critical";
    }

    private String getWellnessMessage(String label) {
        return switch (label) {
            case "Good" -> "Your overall psychological well-being appears to be in a good place.";
            case "Fair" -> "Your well-being is fair, with some areas that could benefit from attention.";
            case "Low" -> "Several aspects of your mental health may need support.";
            default -> "Your results indicate significant distress. We strongly encourage professional support.";
        };
    }

    private record CategoryMeta(String displayName, String description) {}
    private record Recommendation(String summary, String suggestion, boolean professionalCare) {}

    private CategoryMeta getCategoryMeta(String categoryKey) {
        return switch (categoryKey) {
            case QuestionBank.ANXIETY -> new CategoryMeta("Anxiety", "Measures levels of nervousness and fear.");
            case QuestionBank.DEPRESSION -> new CategoryMeta("Depression", "Assesses sadness and low energy.");
            case QuestionBank.STRESS -> new CategoryMeta("Stress", "Evaluates emotional exhaustion.");
            case QuestionBank.SLEEP -> new CategoryMeta("Sleep", "Examines sleep quality.");
            case QuestionBank.THINKING -> new CategoryMeta("Thinking", "Identifies negative thought patterns.");
            case QuestionBank.SOCIAL -> new CategoryMeta("Social", "Gauges loneliness and social withdrawal.");
            case QuestionBank.SELF_WORTH -> new CategoryMeta("Self-Worth", "Reflects self-esteem.");
            default -> new CategoryMeta(categoryKey, "");
        };
    }

    private Recommendation getRecommendation(String severity) {
        return switch (severity) {
            case "Minimal" -> new Recommendation("Healthy range.", "Continue self-care.", false);
            case "Mild" -> new Recommendation("Mild difficulties.", "Consider light self-care strategies.", false);
            case "Moderate" -> new Recommendation("Moderate difficulties.", "Explore structured support.", true);
            default -> new Recommendation("Significant difficulties.", "Speak with a professional.", true);
        };
    }
}