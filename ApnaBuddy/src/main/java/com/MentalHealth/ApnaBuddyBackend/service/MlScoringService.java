package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.MlAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MlScoringService {

    @Value("${app.ml.engine-url}")
    private String PYTHON_ML_URL;

    private final RestTemplate restTemplate;
    private final ChatClient chatClient;

    public MlAnalysisResponse analyzeContext(String currentMessage, List<Message> recentMessages) {
        StringBuilder contextBuilder = new StringBuilder();

        // 1. Build the exact 4+1 Context Window
        // Take up to the last 4 historical messages
        int startIdx = Math.max(0, recentMessages.size() - 4);
        for (int i = startIdx; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            String role = msg.getMessageType().getValue().toUpperCase(); // e.g., USER or ASSISTANT
            contextBuilder.append("[").append(role).append(": ").append(msg.getText()).append("]\n");
        }

        // Attach the current incoming message
        contextBuilder.append("[USER: ").append(currentMessage).append("]");
        String rawContext = contextBuilder.toString();

        // 2. Strict, Emotion-Preserving LLM Translation
        String translationPrompt = """
            You are an automated psychological translation API. Translate the following conversation into English. 
            The text may contain Hinglish, Hindi, or local slang. 
            
            STRICT RULES:
            1. You must perfectly preserve the emotional intensity, sentiment, urgency, and psychological distress of the original text.
            2. Do not soften the words. Do not alter the core meaning.
            3. Output ONLY the raw English translation. Do not include any conversational filler, explanations, or quotes.
            
            Conversation to translate:
            """ + rawContext;

        String translatedContext;
        try {
            translatedContext = chatClient.prompt()
                    .user(translationPrompt)
                    .call()
                    .content();

            // Clean up any accidental whitespace the LLM might add
            translatedContext = translatedContext != null ? translatedContext.trim() : rawContext;
            log.info("🔄 Translated Context -> {}", translatedContext);
        } catch (Exception e) {
            log.error("⚠️ LLM Translation failed. Falling back to raw text.", e);
            translatedContext = rawContext;
        }

        log.info("🚀 OUTGOING TO ML ENGINE -> {}", translatedContext);

        // 3. Call the Python ML Engine
        try {
            Map<String, String> request = Map.of("text", translatedContext);
            MlAnalysisResponse response = restTemplate.postForObject(PYTHON_ML_URL, request, MlAnalysisResponse.class);

            // Null safety check in case the Python server returns an empty body
            if (response == null) {
                throw new IllegalStateException("Python ML Engine returned a null response.");
            }

            log.info("🎯 INCOMING FROM ML ENGINE -> Risk: [{}] | Emotion: [{}] | Sentiment: [{}]",
                    response.risk_level(), response.emotion(), response.sentiment());

            // Pass the translated context back so the Vector Store (RAG) uses pure English for better matching
            return new MlAnalysisResponse(
                    translatedContext,
                    response.risk_level(),
                    response.sentiment(),
                    response.emotion(),
                    response.emotion_score(),
                    response.suicidal_score(),
                    response.depression_score()
            );

        } catch (Exception e) {
            log.error("⚠️ ML Engine unreachable or failed. Falling back to safe Moderate defaults.", e);
            // Fallback ensures the chat doesn't break if your Python server goes offline
            return new MlAnalysisResponse(translatedContext, "Moderate", "Neutral", "Neutral", 0.0, 0.0, 0.0);
        }
    }
}