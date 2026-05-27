package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.GameDtos.*;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.DynamicPromptService;
import com.MentalHealth.ApnaBuddyBackend.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List; // Add this import at the top
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final AuthUtil authUtil;
    private final DynamicPromptService dynamicPromptService;

    // 1. Get a random AI-generated prompt for the Skribble game
    @GetMapping("/skribble/prompts/random")
    public ResponseEntity<PromptResponse> getRandomSkribblePrompt() {
        String aiGeneratedPrompt = dynamicPromptService.generateSkribblePrompt();
        return ResponseEntity.ok(new PromptResponse(aiGeneratedPrompt));
    }

    // 2. Save the finished drawing
    @PostMapping("/skribble")
    public ResponseEntity<SkribbleResponse> saveSkribble(@Valid @RequestBody SkribbleRequest request) {
        Long userId = authUtil.getCurrentUserId();
        SkribbleResponse response = gameService.saveSkribble(userId, request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/skribble")
    public ResponseEntity<List<SkribbleResponse>> getUserSkribbles() {
        Long userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(gameService.getUserSkribbles(userId));
    }
}