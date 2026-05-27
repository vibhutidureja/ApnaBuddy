package com.MentalHealth.ApnaBuddyBackend.util;

import com.MentalHealth.ApnaBuddyBackend.dto.AssessmentDtos.QuestionDto;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QuestionBank {
    public static final String ANXIETY = "anxiety", DEPRESSION = "depression", STRESS = "stress",
            SLEEP = "sleep", THINKING = "thinking", SOCIAL = "social", SELF_WORTH = "selfWorth";

    public static final List<QuestionDto> QUESTIONS = List.of(
            new QuestionDto(1L, ANXIETY, "I feel nervous, anxious, or on edge without a clear reason.", false),
            new QuestionDto(2L, ANXIETY, "I find it difficult to control or stop worrying once it starts.", false),
            new QuestionDto(3L, ANXIETY, "I experience physical symptoms of anxiety such as a racing heart, sweating, or trembling.", false),
            new QuestionDto(4L, ANXIETY, "I avoid situations or places because they make me feel anxious.", false),
            new QuestionDto(5L, ANXIETY, "I feel a sense of impending doom or that something bad is about to happen.", false),
            new QuestionDto(6L, DEPRESSION, "I feel sad, empty, or hopeless about my life or future.", false),
            new QuestionDto(7L, DEPRESSION, "I have lost interest or pleasure in activities I used to enjoy.", false),
            new QuestionDto(8L, DEPRESSION, "I feel worthless or that I am a burden to the people around me.", false),
            new QuestionDto(9L, DEPRESSION, "I have little energy or motivation to get through the day.", false),
            new QuestionDto(10L, DEPRESSION, "I look forward to things in my life and feel moments of genuine joy.", true),
            new QuestionDto(11L, STRESS, "I feel overwhelmed by my responsibilities or to-do list.", false),
            new QuestionDto(12L, STRESS, "I feel emotionally drained and depleted by the end of the day.", false),
            new QuestionDto(13L, STRESS, "I find it hard to relax or unwind even when I have free time.", false),
            new QuestionDto(14L, STRESS, "I feel irritable, short-tempered, or easily frustrated.", false),
            new QuestionDto(15L, STRESS, "I feel like I am running on empty and have nothing left to give.", false),
            new QuestionDto(16L, SLEEP, "I have difficulty falling asleep or staying asleep through the night.", false),
            new QuestionDto(17L, SLEEP, "I wake up feeling unrefreshed, even after a full night's sleep.", false),
            new QuestionDto(18L, SLEEP, "I experience unexplained physical symptoms such as headaches, stomach aches, or muscle tension.", false),
            new QuestionDto(19L, SLEEP, "My sleep patterns have changed significantly (sleeping too much or too little).", false),
            new QuestionDto(20L, SLEEP, "I feel physically well-rested and my body feels comfortable most days.", true),
            new QuestionDto(21L, THINKING, "I have recurring negative thoughts that are difficult to dismiss.", false),
            new QuestionDto(22L, THINKING, "I tend to catastrophise — assuming the worst possible outcome in situations.", false),
            new QuestionDto(23L, THINKING, "I find it hard to concentrate or make decisions.", false),
            new QuestionDto(24L, THINKING, "I blame myself excessively when things go wrong.", false),
            new QuestionDto(25L, THINKING, "I am able to challenge unhelpful thoughts and see situations more clearly.", true),
            new QuestionDto(26L, SOCIAL, "I feel lonely or isolated, even when people are around.", false),
            new QuestionDto(27L, SOCIAL, "I withdraw from social situations or avoid spending time with others.", false),
            new QuestionDto(28L, SOCIAL, "I feel like no one truly understands or cares about how I feel.", false),
            new QuestionDto(29L, SOCIAL, "I have at least one person I can talk to openly when I am struggling.", true),
            new QuestionDto(30L, SOCIAL, "My relationships feel supportive and fulfilling.", true),
            new QuestionDto(31L, SELF_WORTH, "I feel good about myself and my abilities.", true),
            new QuestionDto(32L, SELF_WORTH, "I struggle to complete everyday tasks such as personal hygiene, meals, or household chores.", false),
            new QuestionDto(33L, SELF_WORTH, "I compare myself negatively to others and feel like I fall short.", false),
            new QuestionDto(34L, SELF_WORTH, "I have a clear sense of purpose or meaning in my daily life.", true),
            new QuestionDto(35L, SELF_WORTH, "I feel capable of handling the challenges that come my way.", true)
    );

    public static final Map<Long, QuestionDto> QUESTION_MAP = QUESTIONS.stream()
            .collect(Collectors.toMap(QuestionDto::id, Function.identity()));
}