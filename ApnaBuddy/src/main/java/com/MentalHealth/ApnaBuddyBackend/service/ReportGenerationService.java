package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.ReportDtos.*;
import com.MentalHealth.ApnaBuddyBackend.entity.ActivityLog;
import com.MentalHealth.ApnaBuddyBackend.entity.AssessmentRecord;
import com.MentalHealth.ApnaBuddyBackend.entity.ChatSession;
import com.MentalHealth.ApnaBuddyBackend.repository.ActivityLogRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.AssessmentRecordRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationService {

    private final AssessmentRecordRepository assessmentRepo;
    private final ChatSessionRepository chatSessionRepo;
    private final ActivityLogRepository activityLogRepo;
    private final ChatClient chatClient;

    public WellnessReportResponse generate30DayReport(Long userId) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        // 1. Fetch Data
        List<AssessmentRecord> assessments = assessmentRepo.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(a -> a.getCreatedAt().isAfter(thirtyDaysAgo)).toList();
        List<ChatSession> sessions = chatSessionRepo.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId).stream()
                .filter(s -> s.getCreatedAt().isAfter(thirtyDaysAgo)).toList();
        List<ActivityLog> activities = activityLogRepo.findByUserId(userId).stream()
                .filter(a -> a.getCompletedAt().isAfter(thirtyDaysAgo)).toList();

        // 2. Calculate Graph Data
        List<TrendPoint> wellnessTrend = calculateWellnessTrend(assessments);
        List<TrendPoint> chatTrend = calculateChatMoodTrend(sessions); // NEW
        Map<String, Integer> emotions = calculateComposition(sessions, true);
        Map<String, Integer> sentiments = calculateComposition(sessions, false); // NEW
        List<ActivityImpact> efficacy = calculateCopingEfficacy(activities);

        // 3. Generate AI Narrative
        AiReportNarrative narrative = generateAiInsights(assessments, sessions);

        return new WellnessReportResponse(
                narrative.moodSummary(), // NEW
                narrative.summaryNarrative(),
                narrative.triggersAndGrowth(),
                narrative.recommendations(),
                wellnessTrend,
                chatTrend, // NEW
                emotions,
                sentiments, // NEW
                efficacy
        );
    }

    private AiReportNarrative generateAiInsights(List<AssessmentRecord> assessments, List<ChatSession> sessions) {
        String sessionSummaries = sessions.stream()
                .map(s -> "Risk: " + s.getMaxRiskLevel() + " | Emotion: " + s.getOverallEmotion() + " | Sentiment: " + s.getOverallSentiment())
                .collect(Collectors.joining("\n"));

        double avgAssessment = assessments.stream().mapToInt(AssessmentRecord::getWellnessScore).average().orElse(0.0);

        String promptTemplate = """
                You are an empathetic wellness companion. Review the user's data for the last 30 days.
                
                CRITICAL RULES:
                1. DO NOT diagnose medical conditions.
                2. Be encouraging but objective.
                
                DATA CONTEXT:
                Average Assessment Score: %.2f / 140
                Recent Chat Themes & Risks:
                %s
                
                Please generate:
                1. moodSummary: A short, 1-2 sentence quick summary of their overall mood and trajectory.
                2. summaryNarrative: A detailed paragraph on their emotional state.
                3. triggersAndGrowth: Patterns you noticed.
                4. recommendations: Actionable self-care advice.
                """;

        String finalPrompt = String.format(promptTemplate, avgAssessment, sessionSummaries);
        BeanOutputConverter<AiReportNarrative> converter = new BeanOutputConverter<>(AiReportNarrative.class);

        String aiResponseText = chatClient.prompt()
                .system(finalPrompt)
                .user("Generate my 30-day wellness insights. " + converter.getFormat())
                .call().content();

        return converter.convert(aiResponseText);
    }

    // --- MATH HELPERS ---
    private List<TrendPoint> calculateWellnessTrend(List<AssessmentRecord> assessments) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.systemDefault());
        return assessments.stream().map(a -> new TrendPoint(fmt.format(a.getCreatedAt()), a.getWellnessScore())).collect(Collectors.toList());
    }

    // NEW: Extracts the depression score from chats, scales it to 0-100 for the graph
    private List<TrendPoint> calculateChatMoodTrend(List<ChatSession> sessions) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.systemDefault());
        List<ChatSession> chronological = new ArrayList<>(sessions);
        Collections.reverse(chronological); // Needs to be oldest to newest for the graph

        return chronological.stream()
                .filter(s -> s.getAverageDepressionScore() != null)
                .map(s -> new TrendPoint(fmt.format(s.getUpdatedAt()), (int)(s.getAverageDepressionScore() * 100)))
                .collect(Collectors.toList());
    }

    // UPDATED: Handles both Emotion and Sentiment pie charts
    private Map<String, Integer> calculateComposition(List<ChatSession> sessions, boolean isEmotion) {
        Map<String, Integer> composition = new HashMap<>();
        for (ChatSession s : sessions) {
            String value = isEmotion ? s.getOverallEmotion() : s.getOverallSentiment();
            if (value == null || value.trim().isEmpty()) value = "Neutral";
            composition.put(value, composition.getOrDefault(value, 0) + 1);
        }
        return composition;
    }

    private List<ActivityImpact> calculateCopingEfficacy(List<ActivityLog> activities) {
        Map<String, List<Integer>> impactMap = new HashMap<>();
        for (ActivityLog act : activities) {
            int improvement = act.getMoodAfter() - act.getMoodBefore();
            impactMap.computeIfAbsent(act.getActivityType(), k -> new ArrayList<>()).add(improvement);
        }
        return impactMap.entrySet().stream()
                .map(e -> new ActivityImpact(e.getKey(), e.getValue().stream().mapToInt(i -> i).average().orElse(0.0)))
                .collect(Collectors.toList());
    }
}