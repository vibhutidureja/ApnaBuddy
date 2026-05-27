package com.MentalHealth.ApnaBuddyBackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@Slf4j
public class AiToolsConfig {

    public record TriggerUiRequest(String actionType, String reason) {}
    public record TriggerUiResponse(String status, String instructionForAi) {}

    @Bean
    @Description("Triggers a visual grounding or breathing exercise on the user's mobile screen. Use this ONLY if the user is exhibiting high anxiety, panic, or requests a physical exercise.")
    public Function<TriggerUiRequest, TriggerUiResponse> triggerMobileUiExercise() {
        return request -> {
            log.warn("🚨 AI autonomously triggered Mobile UI action: {} for reason: {}", request.actionType(), request.reason());

            // --- THE FIX: Force the AI to embed a secret flag in its response ---
            return new TriggerUiResponse(
                    "SUCCESS",
                    "The UI has been triggered. You MUST include the exact string '[ACTION:BREATHING]' somewhere in your final text response to the user. Do not forget this string."
            );
        };
    }
}