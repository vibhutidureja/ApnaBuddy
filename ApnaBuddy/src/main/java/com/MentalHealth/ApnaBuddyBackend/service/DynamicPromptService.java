package com.MentalHealth.ApnaBuddyBackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicPromptService {

    private final ChatClient chatClient;
    private final Random random = new Random();

    // Themes to keep the AI focused and varied
    private static final List<String> JOURNAL_THEMES = List.of(
            "gratitude", "overcoming a challenge", "inner child", "setting boundaries",
            "self-forgiveness", "identifying a safe space", "small daily wins"
    );

    private static final List<String> SKRIBBLE_THEMES = List.of(
            "a peaceful environment", "a funny or silly animal", "an abstract representation of calmness",
            "a childhood memory", "a visualization of letting go", "a dream vacation"
    );

    public String generateJournalPrompt() {
        String theme = JOURNAL_THEMES.get(random.nextInt(JOURNAL_THEMES.size()));
        String systemInstruction = """
                You are a CBT therapist creating a journaling prompt for a student.
                Generate a single, thoughtful journaling question based on the theme of: %s.
                CRITICAL: Return ONLY the question itself. No quotes, no intro, no extra text.
                """;

        return chatClient.prompt()
                .system(String.format(systemInstruction, theme))
                .user("Generate the prompt.")
                .call()
                .content()
                .replace("\"", "") // Clean up in case the LLM adds quotes
                .trim();
    }

    public String generateSkribblePrompt() {
        String theme = SKRIBBLE_THEMES.get(random.nextInt(SKRIBBLE_THEMES.size()));
        String systemInstruction = """
                You are an art therapist creating a drawing prompt for a relaxation game called Skribble.
                Generate a single, creative drawing instruction based on the theme of: %s.
                Make it simple enough to draw on a mobile phone in 3 minutes.
                CRITICAL: Return ONLY the instruction itself. Start with 'Draw...'. No quotes, no extra text.
                """;

        return chatClient.prompt()
                .system(String.format(systemInstruction, theme))
                .user("Generate the drawing prompt.")
                .call()
                .content()
                .replace("\"", "")
                .trim();
    }
}