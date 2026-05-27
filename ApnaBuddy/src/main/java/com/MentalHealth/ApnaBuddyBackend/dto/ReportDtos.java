package com.MentalHealth.ApnaBuddyBackend.dto;

import java.util.List;
import java.util.Map;

public class ReportDtos {

    public record WellnessReportResponse(
            // AI Generated Text
            String moodSummary,          // NEW: Short 1-2 sentence summary
            String summaryNarrative,
            String triggersAndGrowth,
            String recommendations,

            // Hard Math Data for Graphs
            List<TrendPoint> wellnessTrend,
            List<TrendPoint> chatMoodTrend,         // NEW: Depression score trend from chats
            Map<String, Integer> emotionComposition,
            Map<String, Integer> sentimentComposition, // NEW: Positive/Neutral/Negative split
            List<ActivityImpact> copingEfficacy
    ) {}

    public record TrendPoint(String date, Integer score) {}
    public record ActivityImpact(String activity, Double improvement) {}

    public record AiReportNarrative(
            String moodSummary, // NEW
            String summaryNarrative,
            String triggersAndGrowth,
            String recommendations
    ) {}
}