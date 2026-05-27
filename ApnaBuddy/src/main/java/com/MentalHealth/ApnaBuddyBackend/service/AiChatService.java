package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.MlAnalysisResponse;
import com.MentalHealth.ApnaBuddyBackend.entity.ActivityLog;
import com.MentalHealth.ApnaBuddyBackend.entity.AssessmentRecord;
import com.MentalHealth.ApnaBuddyBackend.entity.ChatSession;
import com.MentalHealth.ApnaBuddyBackend.repository.ActivityLogRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.AssessmentRecordRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.ChatSessionRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.UserOnboardingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiChatService {

    public record AiResponseData(String content, Integer tokensUsed, String actionToTrigger) {}

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore globalVectorStore;
    private final VectorStore userVectorStore;
    private final MlScoringService mlScoringService;
    private final ChatDbService chatDbService;
    private final ChatSessionRepository chatSessionRepository; // NEW: To fetch rolling session metrics
    private final UserOnboardingRepository onboardingRepo;
    private final AssessmentRecordRepository assessmentRepo;
    private final ActivityLogRepository activityLogRepo;

    public AiChatService(
            ChatClient chatClient,
            ChatMemory chatMemory,
            @Qualifier("globalVectorStore") VectorStore globalVectorStore,
            @Qualifier("userVectorStore") VectorStore userVectorStore,
            MlScoringService mlScoringService,
            ChatDbService chatDbService,
            ChatSessionRepository chatSessionRepository,
            UserOnboardingRepository onboardingRepo,
            AssessmentRecordRepository assessmentRepo,
            ActivityLogRepository activityLogRepo) {

        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.globalVectorStore = globalVectorStore;
        this.userVectorStore = userVectorStore;
        this.mlScoringService = mlScoringService;
        this.chatDbService = chatDbService;
        this.chatSessionRepository = chatSessionRepository;
        this.onboardingRepo = onboardingRepo;
        this.assessmentRepo = assessmentRepo;
        this.activityLogRepo = activityLogRepo;
    }

    // --- ENHANCED CBT PROMPT WITH SESSION ANALYTICS ---
    private static final String THERAPY_SYSTEM_TEMPLATE = """
            You are 'Stillwater', an expert Cognitive Behavioral Therapist (CBT) and deeply empathetic mental health companion.
            Your primary goal is to provide a safe, non-judgmental space, helping the user navigate their emotions using evidence-based CBT techniques.
            
            === 1. USER PERSONA & PREFERENCES ===
            Name/Nickname: %s
            Background: %s
            Goals for Therapy: %s
            Chat Style Preference: %s (CRITICAL: You MUST adopt this tone natively)
            Known Coping Habits: %s
            
            === 2. CLINICAL & WELLNESS CONTEXT ===
            Latest Assessment: %s
            Recent In-App Activity: %s
            
            === 3. LIVE ML TELEMETRY (Current State) ===
            - Detected Emotion: %s
            - Overall Sentiment: %s
            - AI Evaluated Risk Level: %s
            
            === 4. SESSION-WIDE TRENDS (Historical Perspective) ===
            (Use this to maintain perspective. If the session average depression is high, remain vigilant even if the current message is positive)
            - Avg Depression Score this session: %s
            - Highest Risk reached this session: %s
            - Dominant Emotional Vibe: %s
            
            === 5. WORKING MEMORY & THERAPEUTIC STRATEGY ===
            (This includes past triggers from User RAG, and clinical strategies from Global RAG)
            %s
            
            === YOUR DIRECTIVES (STRICTLY ENFORCED) ===
            1. TONE & EMPATHY: Mirror the user's preferred chat style, but ground your therapeutic interventions in the Global RAG strategy. 
            2. NATURAL MEMORY USE: Weave in the 'Working Memory' and 'Persona' naturally. Never say "According to your file" or "I see in my database". Act as if you simply remember them.
            3. CRISIS PROTOCOL: If the 'Current Risk' OR 'Highest Risk' is HIGH or CRITICAL, immediately drop standard CBT homework. Focus 100%% on grounding, safety, de-escalation, and encouraging them to use the emergency features.
            4. PACING: Keep responses concise and conversational. Do NOT overwhelm the user with long lists of instructions.
            5. ENGAGEMENT: End your response with only ONE gentle, open-ended question to guide their reflection.
            """;

    @Async
    public void saveToUserLongTermMemory(String userId, String userMessage, String aiResponse) {
        try {
            String combinedContent = "User: " + userMessage + "\nAI: " + aiResponse;
            Document memoryChunk = new Document(combinedContent, Map.of("userId", userId));
            userVectorStore.add(List.of(memoryChunk));
        } catch (Exception e) {
            log.error("Failed to save long-term memory for user {}: {}", userId, e.getMessage());
        }
    }

    public AiResponseData chatWithUser(String prompt, String springAiConversationId, String userId, Long dbSessionId) {

        Long parsedUserId = Long.parseLong(userId);

        // 1. Get Short-Term Memory (Recent chat history)
        List<Message> recentMessages = chatMemory.get(springAiConversationId);
        boolean isStartOfSession = recentMessages.size() < 4;

        // 2. Live ML Analysis & Rolling Update
        MlAnalysisResponse mlData = mlScoringService.analyzeContext(prompt, recentMessages);

        // 🔥 UPDATE: Call the new, comprehensive ML metrics updater
        chatDbService.updateMlRollingMetrics(dbSessionId, mlData);

        String searchTarget = (mlData.translated_text() != null && !mlData.translated_text().isBlank()) ? mlData.translated_text() : prompt;
        String activeStrategy = chatDbService.getActiveStrategy(dbSessionId);
        String riskLevel = mlData.risk_level() != null ? mlData.risk_level() : "Low";

        // 3. DYNAMIC CONTEXT SWITCHING LOGIC (RAG)
        boolean needsDeepContext = isStartOfSession ||
                riskLevel.equalsIgnoreCase("High") ||
                riskLevel.equalsIgnoreCase("CRITICAL");

        if (needsDeepContext) {
            log.info("Deep Context Triggered. Reason - Start of Session: {}, High Risk: {}", isStartOfSession, riskLevel);
            String rawUser = getUserLongTermContext(searchTarget, userId);
            String rawGlobal = getGlobalTherapyContext(searchTarget);

            // 🔥 UPDATE: Explicitly separate Tone (Global) from Triggers (User)
            activeStrategy = "PAST KNOWN TRIGGERS (User History):\n" + rawUser + "\n\nRECOMMENDED CBT STRATEGY & TONE (Global Knowledge):\n" + rawGlobal;
            chatDbService.updateActiveStrategy(dbSessionId, activeStrategy);
        } else {
            if (activeStrategy == null || activeStrategy.isBlank()) {
                activeStrategy = "Maintain empathetic listening and use CBT to explore the user's current feelings. Continue the current flow.";
            }
        }

        // 4. FETCH ROLLING SESSION ANALYTICS (For Section 4 of Prompt)
        String avgDep = "0.0";
        String maxRisk = "Low";
        String vibe = "Neutral";

        var sessionOpt = chatSessionRepository.findById(dbSessionId);
        if (sessionOpt.isPresent()) {
            ChatSession s = sessionOpt.get();
            avgDep = String.format("%.2f", s.getAverageDepressionScore() != null ? s.getAverageDepressionScore() : 0.0);
            maxRisk = s.getMaxRiskLevel() != null ? s.getMaxRiskLevel() : "Low";
            vibe = s.getOverallEmotion() != null ? s.getOverallEmotion() : "Balanced";
        }

        // 5. SAFELY FETCH USER PERSONA
        String nickname = "User", background = "Unknown", goals = "General Support", chatPreference = "Empathetic and Friendly", coping = "Unknown";
        try {
            var onboardingOpt = onboardingRepo.findByUserId(parsedUserId);
            if (onboardingOpt.isPresent()) {
                var o = onboardingOpt.get();
                nickname = o.getNickname() != null && !o.getNickname().isBlank() ? o.getNickname() : "User";
                String age = o.getAgeGroup() != null && !o.getAgeGroup().isBlank() ? o.getAgeGroup() : "Unknown age";
                String gender = o.getGender() != null && !o.getGender().isBlank() ? o.getGender() : "Unspecified";
                String occ = o.getOccupation() != null && !o.getOccupation().isBlank() ? o.getOccupation() : "Unknown occupation";
                String concerns = o.getPrimaryConcerns() != null && !o.getPrimaryConcerns().isBlank() ? o.getPrimaryConcerns() : "None stated";
                String feeling = o.getFeelingToday() != null && !o.getFeelingToday().isBlank() ? o.getFeelingToday() : "Unknown";
                background = String.format("Age: %s, Gender: %s | Occ: %s | Main Concerns: %s | Stated feeling at onboarding: %s", age, gender, occ, concerns, feeling);
                goals = o.getGoals() != null && !o.getGoals().isBlank() ? o.getGoals() : "General Support";
                chatPreference = o.getChatPreference() != null && !o.getChatPreference().isBlank() ? o.getChatPreference() : "Empathetic and Friendly";
                coping = o.getCopingMechanisms() != null && !o.getCopingMechanisms().isBlank() ? o.getCopingMechanisms() : "Unknown";
            }
        } catch (Exception e) {
            log.warn("Could not fetch onboarding data for user {}: {}", userId, e.getMessage());
        }

        // 6. SAFELY FETCH LATEST ASSESSMENT
        String clinicalAssessment = "No assessment on file yet.";
        try {
            List<AssessmentRecord> assessments = assessmentRepo.findByUserIdOrderByCreatedAtDesc(parsedUserId);
            if (!assessments.isEmpty()) {
                AssessmentRecord latest = assessments.get(0);
                clinicalAssessment = String.format("Overall Wellness: %s (Score: %d/140). Professional Support Needed: %s",
                        latest.getWellnessResult(), latest.getWellnessScore(), latest.getRequiresProfessionalSupport());
            }
        } catch (Exception e) {
            log.warn("Could not fetch assessment data for user {}: {}", userId, e.getMessage());
        }

        // 7. SAFELY FETCH RECENT ACTIVITY
        String recentActivityContext = "No side-activities completed today.";
        try {
            var latestActivity = activityLogRepo.findFirstByUserIdOrderByCompletedAtDesc(parsedUserId);
            if (latestActivity.isPresent()) {
                ActivityLog logRecord = latestActivity.get();
                recentActivityContext = String.format("User recently completed a %s exercise for %d seconds. Their mood changed from %d/10 to %d/10.",
                        logRecord.getActivityType(), logRecord.getDurationSeconds(), logRecord.getMoodBefore(), logRecord.getMoodAfter());
            }
        } catch (Exception e) {
            log.warn("Could not fetch recent activity for user {}: {}", userId, e.getMessage());
        }

        // 8. BUILD FINAL PROMPT (Fills the 14 slots in the template)
        String systemPrompt = String.format(THERAPY_SYSTEM_TEMPLATE,
                nickname, background, goals, chatPreference, coping,
                clinicalAssessment, recentActivityContext,
                mlData.emotion(), mlData.sentiment(), riskLevel,
                avgDep, maxRisk, vibe,
                activeStrategy);

        // 9. CALL AI
        ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .toolNames("triggerMobileUiExercise")
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(springAiConversationId)
                        .build())
                .call()
                .chatResponse();

        String aiText = response.getResult().getOutput().getText();

        int tokensUsed = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
            tokensUsed = totalTokens != null ? totalTokens : 0;
        }

        // 10. INTERCEPT UI TRIGGERS
        String triggeredAction = null;
        if (aiText != null && aiText.contains("[ACTION:BREATHING]")) {
            triggeredAction = "BREATHING_EXERCISE";
            aiText = aiText.replace("[ACTION:BREATHING]", "").trim();
        }

        return new AiResponseData(aiText, tokensUsed, triggeredAction);
    }

    private String getGlobalTherapyContext(String query) {
        try {
            List<Document> docs = globalVectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build());
            return formatContext(docs, "No specific global examples needed at this moment. Maintain professional empathy.");
        } catch (Exception e) {
            log.error("Global Vector Store search failed: {}", e.getMessage());
            return "Global CBT context currently unavailable.";
        }
    }

    private String getUserLongTermContext(String query, String userId) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            List<Document> docs = userVectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(3)
                    .filterExpression(b.eq("userId", userId).build())
                    .build());
            return formatContext(docs, "No relevant long-term history found.");
        } catch (Exception e) {
            log.error("User Vector Store search failed for user {}: {}", userId, e.getMessage());
            return "User long-term memory currently unavailable.";
        }
    }

    private String formatContext(List<Document> docs, String fallback) {
        if (docs == null || docs.isEmpty()) return fallback;
        return docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    }
}