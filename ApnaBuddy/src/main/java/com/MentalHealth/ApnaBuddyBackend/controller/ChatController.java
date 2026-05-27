package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.ChatRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.ChatResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.MessageResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.SessionResponse;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.AiChatService;
import com.MentalHealth.ApnaBuddyBackend.service.ChatDbService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AuthUtil authUtil;
    private final AiChatService aiChatService;
    private final ChatDbService chatDbService;

    @GetMapping("/sessions/latest")
    public ResponseEntity<SessionResponse> getOrCreateLatestSession() {
        Long userId = authUtil.getCurrentUserId();
        SessionResponse latestSession = chatDbService.getLatestSessionOrCreate(userId);
        return ResponseEntity.ok(latestSession);
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createNewSession() {
        Long userId = authUtil.getCurrentUserId();
        SessionResponse newSession = chatDbService.createSession(userId);
        return ResponseEntity.ok(newSession);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getUserSessions() {
        Long userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(chatDbService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageResponse>> getSessionMessages(@PathVariable Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(chatDbService.getSessionMessages(sessionId, userId));
    }

    @PostMapping("/sessions/{sessionId}/ask")
    public ResponseEntity<ChatResponse> askAi(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest request) {

        Long userId = authUtil.getCurrentUserId();
        String userPrompt = request.prompt();

        String springAiConvId = chatDbService.verifySessionAndGetSpringAiId(sessionId, userId);
        chatDbService.saveUserMessage(sessionId, userPrompt);

        AiChatService.AiResponseData aiData = aiChatService.chatWithUser(userPrompt, springAiConvId, String.valueOf(userId), sessionId);

        chatDbService.saveAssistantMessage(sessionId, aiData.content(), aiData.tokensUsed());

        aiChatService.saveToUserLongTermMemory(String.valueOf(userId), userPrompt, aiData.content());

        return ResponseEntity.ok(new ChatResponse(aiData.content(), aiData.actionToTrigger()));    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<String> endAndDeleteSession(@PathVariable Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        chatDbService.deleteSession(sessionId, userId);
        return ResponseEntity.ok("Session ended and wiped successfully.");
    }
}