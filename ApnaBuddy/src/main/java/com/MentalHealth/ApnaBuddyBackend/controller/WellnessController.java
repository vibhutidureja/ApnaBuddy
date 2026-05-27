package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.GameDtos.PromptResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.ReportDtos;
import com.MentalHealth.ApnaBuddyBackend.dto.WellnessDtos.*;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.DynamicPromptService;
import com.MentalHealth.ApnaBuddyBackend.service.ReportGenerationService;
import com.MentalHealth.ApnaBuddyBackend.service.WellnessActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessActivityService wellnessService;
    private final DynamicPromptService dynamicPromptService;
    private final AuthUtil authUtil;
    private final ReportGenerationService reportGenerationService;

    @PostMapping("/activity")
    public ResponseEntity<String> logActivity(@Valid @RequestBody ActivityLogRequest request) {
        Long userId = authUtil.getCurrentUserId();
        wellnessService.logActivity(userId, request);
        return ResponseEntity.ok("Activity logged successfully.");
    }

    // --- Create Journal ---
    @PostMapping("/journal")
    public ResponseEntity<JournalResponse> saveJournal(@Valid @RequestBody JournalRequest request) {
        Long userId = authUtil.getCurrentUserId();
        JournalResponse response = wellnessService.saveJournal(userId, request);
        return ResponseEntity.ok(response);
    }

    // --- NEW: Get Journal History ---
    @GetMapping("/journal")
    public ResponseEntity<List<JournalResponse>> getJournals() {
        Long userId = authUtil.getCurrentUserId();
        List<JournalResponse> history = wellnessService.getUserJournals(userId);
        return ResponseEntity.ok(history);
    }

    // --- NEW: Update Existing Journal ---
    @PutMapping("/journal/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request) {
        Long userId = authUtil.getCurrentUserId();
        JournalResponse response = wellnessService.updateJournal(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/journal/prompts/random")
    public ResponseEntity<PromptResponse> getRandomJournalPrompt() {
        String aiGeneratedPrompt = dynamicPromptService.generateJournalPrompt();
        return ResponseEntity.ok(new PromptResponse(aiGeneratedPrompt));
    }

    @PostMapping("/fire-game")
    public ResponseEntity<String> playFireGame(@Valid @RequestBody FireGameRequest request) {
        Long userId = authUtil.getCurrentUserId();
        wellnessService.logFireGame(userId, request);
        return ResponseEntity.ok("Thought successfully burned and activity logged.");
    }
    @GetMapping("/report")
    public ResponseEntity<ReportDtos.WellnessReportResponse> getMonthlyReport() {
        Long userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(reportGenerationService.generate30DayReport(userId));
    }
}