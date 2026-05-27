package com.MentalHealth.ApnaBuddyBackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
public class AIConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build(); // We dynamically set the system prompt in the service now!
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repo) {
        // Keeps the last 10 messages of short-term context per session ID
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(10)
                .build();
    }
}