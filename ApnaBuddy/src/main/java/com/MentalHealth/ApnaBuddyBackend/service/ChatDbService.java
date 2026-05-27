package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.MessageResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.MlAnalysisResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.SessionResponse;
import com.MentalHealth.ApnaBuddyBackend.entity.ChatMessage;
import com.MentalHealth.ApnaBuddyBackend.entity.ChatSession;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.enums.MessageRole;
import com.MentalHealth.ApnaBuddyBackend.repository.ChatMessageRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.ChatSessionRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatDbService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found or unauthorized"));

        chatMessageRepository.deleteByChatSessionId(sessionId);
        session.setDeletedAt(Instant.now());
        chatSessionRepository.save(session);
    }

    /**
     * Updates all ML metrics for the session. This data is the foundation
     * for the Wellness Report generation.
     */
    @Transactional
    public void updateMlRollingMetrics(Long sessionId, MlAnalysisResponse mlData) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            int currentCount = session.getInteractionCount() == null ? 0 : session.getInteractionCount();
            session.setInteractionCount(currentCount + 1);

            // 1. Update Suicide Metrics
            double cumSuicide = session.getCumulativeSuicideScore() == null ? 0.0 : session.getCumulativeSuicideScore();
            session.setCumulativeSuicideScore(cumSuicide + mlData.suicidal_score());
            session.setAverageSuicideScore(session.getCumulativeSuicideScore() / session.getInteractionCount());

            // 2. Update Depression Metrics
            double cumDep = session.getCumulativeDepressionScore() == null ? 0.0 : session.getCumulativeDepressionScore();
            session.setCumulativeDepressionScore(cumDep + mlData.depression_score());
            session.setAverageDepressionScore(session.getCumulativeDepressionScore() / session.getInteractionCount());

            // 3. Track Peak Risk (High-water mark for the session)
            if (isHigherRisk(mlData.risk_level(), session.getMaxRiskLevel())) {
                session.setMaxRiskLevel(mlData.risk_level());
            }

            // 4. Update Dominant Emotion & Sentiment
            // We store the most recent high-confidence emotion/sentiment
            session.setOverallEmotion(mlData.emotion());
            session.setOverallSentiment(mlData.sentiment());

            chatSessionRepository.save(session);
        });
    }

    /**
     * Helper to compare risk levels: Low < Moderate < High < CRITICAL
     */
    private boolean isHigherRisk(String newRisk, String currentMax) {
        if (currentMax == null) return true;
        List<String> riskHierarchy = Arrays.asList("Low", "Moderate", "High", "CRITICAL");
        return riskHierarchy.indexOf(newRisk) > riskHierarchy.indexOf(currentMax);
    }

    @Transactional(readOnly = true)
    public String getActiveStrategy(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .map(ChatSession::getActiveStrategyContext)
                .orElse(null);
    }

    @Transactional
    public void updateActiveStrategy(Long sessionId, String newStrategy) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setActiveStrategyContext(newStrategy);
            chatSessionRepository.save(session);
        });
    }

    @Transactional
    public SessionResponse getLatestSessionOrCreate(Long userId) {
        Optional<ChatSession> latest = chatSessionRepository.findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId);

        if (latest.isPresent()) {
            ChatSession session = latest.get();
            long diff = Duration.between(session.getUpdatedAt(), Instant.now()).toMinutes();

            if (diff >= 20) {
                log.info("Session {} inactive for 20+ mins. Wiping raw messages and soft-deleting.", session.getId());
                chatMessageRepository.deleteByChatSessionId(session.getId());
                session.setDeletedAt(Instant.now());
                chatSessionRepository.save(session);
                return createSession(userId);
            }
            return mapToSessionResponse(session);
        }

        return createSession(userId);
    }

    @Transactional
    public SessionResponse createSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatSession newSession = ChatSession.builder()
                .user(user)
                .springAiConversationId(UUID.randomUUID().toString())
                .sessionTitle("New Conversation")
                .interactionCount(0)
                .cumulativeSuicideScore(0.0)
                .averageSuicideScore(0.0)
                .cumulativeDepressionScore(0.0)
                .averageDepressionScore(0.0)
                .maxRiskLevel("Low")
                .totalTokensUsed(0)
                .build();

        newSession = chatSessionRepository.save(newSession);
        return mapToSessionResponse(newSession);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getSessionMessages(Long sessionId, Long userId) {
        verifySessionAndGetSpringAiId(sessionId, userId);
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public String verifySessionAndGetSpringAiId(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found or unauthorized"));

        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        return session.getSpringAiConversationId();
    }

    @Transactional
    public void saveUserMessage(Long sessionId, String content) {
        saveMessage(sessionId, content, MessageRole.USER, 0);
    }

    @Transactional
    public void saveAssistantMessage(Long sessionId, String content, Integer tokensUsed) {
        saveMessage(sessionId, content, MessageRole.ASSISTANT, tokensUsed);
    }

    private void saveMessage(Long sessionId, String content, MessageRole role, Integer tokensUsed) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .role(role)
                .content(content)
                .tokensUsed(tokensUsed)
                .build();

        chatMessageRepository.save(message);

        if (tokensUsed != null && tokensUsed > 0) {
            int currentTotal = session.getTotalTokensUsed() == null ? 0 : session.getTotalTokensUsed();
            session.setTotalTokensUsed(currentTotal + tokensUsed);
        }

        if (role == MessageRole.USER && "New Conversation".equals(session.getSessionTitle())) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            session.setSessionTitle(title);
        }

        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);
    }

    private SessionResponse mapToSessionResponse(ChatSession session) {
        return new SessionResponse(session.getId(), session.getSessionTitle(), session.getCreatedAt());
    }
}