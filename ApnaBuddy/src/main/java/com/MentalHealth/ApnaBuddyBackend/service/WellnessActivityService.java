package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.WellnessDtos.*;
import com.MentalHealth.ApnaBuddyBackend.entity.ActivityLog;
import com.MentalHealth.ApnaBuddyBackend.entity.JournalEntry;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.ActivityLogRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WellnessActivityService {

    private final ActivityLogRepository activityLogRepo;
    private final JournalEntryRepository journalRepo;
    private final UserRepository userRepository;
    private final MlScoringService mlScoringService;

    @Transactional
    public void logActivity(Long userId, ActivityLogRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        ActivityLog logEntity = ActivityLog.builder()
                .user(user)
                .activityType(request.activityType())
                .durationSeconds(request.durationSeconds())
                .moodBefore(request.moodBefore())
                .moodAfter(request.moodAfter())
                .build();

        activityLogRepo.save(logEntity);
        log.info("Activity logged for user {}: {}", userId, request.activityType());
    }

    @Transactional
    public JournalResponse saveJournal(Long userId, JournalRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        // Pass an empty list for recent messages to evaluate just the journal entry
        var analysis = mlScoringService.analyzeContext(request.content(), List.of());

        JournalEntry entry = JournalEntry.builder()
                .user(user)
                .journalType(request.journalType())
                .content(request.content())
                .promptUsed(request.promptUsed())
                .sentiment(analysis.sentiment())
                .emotionScore(analysis.emotion_score())
                .build();

        entry = journalRepo.save(entry);

        return new JournalResponse(
                entry.getId(), entry.getJournalType(), entry.getContent(),
                entry.getPromptUsed(), entry.getSentiment(), entry.getCreatedAt()
        );
    }

    // --- NEW: Fetch Journal History ---
    @Transactional(readOnly = true)
    public List<JournalResponse> getUserJournals(Long userId) {
        return journalRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entry -> new JournalResponse(
                        entry.getId(), entry.getJournalType(), entry.getContent(),
                        entry.getPromptUsed(), entry.getSentiment(), entry.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // --- NEW: Update Existing Journal ---
    @Transactional
    public JournalResponse updateJournal(Long userId, Long journalId, JournalRequest request) {
        JournalEntry entry = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        // Security check: Ensure the user owns this journal
        if (!entry.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to edit this journal");
        }

        // Re-analyze sentiment on updated content
        var analysis = mlScoringService.analyzeContext(request.content(), List.of());

        entry.setContent(request.content());
        entry.setSentiment(analysis.sentiment());
        entry.setEmotionScore(analysis.emotion_score());

        entry = journalRepo.save(entry);

        return new JournalResponse(
                entry.getId(), entry.getJournalType(), entry.getContent(),
                entry.getPromptUsed(), entry.getSentiment(), entry.getCreatedAt()
        );
    }

    // --- Fire Game Logic ---
    @Transactional
    public void logFireGame(Long userId, FireGameRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        ActivityLog logEntity = ActivityLog.builder()
                .user(user)
                .activityType("FIRE_GAME")
                .durationSeconds(0)
                .moodBefore(request.moodBefore())
                .moodAfter(request.moodAfter())
                .build();

        activityLogRepo.save(logEntity);

        try {
            var analysis = mlScoringService.analyzeContext(request.thoughtToBurn(), List.of());
            log.info("User {} burned a thought. Sentiment: {}, Risk Level: {}",
                    userId, analysis.sentiment(), analysis.risk_level());
        } catch (Exception e) {
            log.warn("Could not analyze burned thought for user {}", userId);
        }
    }
}