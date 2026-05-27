package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.OnboardingProfileResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.OnboardingRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.ProfileUpdateRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.UserProfileResponse;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.entity.UserOnboarding;
import com.MentalHealth.ApnaBuddyBackend.repository.UserOnboardingRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserOnboardingRepository onboardingRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getName(),
                user.getPictureUrl(), user.getHasCompletedOnboarding(), user.getHasCompletedAssessment()
        );
    }

    // --- NEW: Fetch Onboarding Profile details for the React Native Screen ---
    @Transactional(readOnly = true)
    public OnboardingProfileResponse getOnboardingProfile(Long userId) {
        return onboardingRepository.findByUserId(userId)
                .map(o -> new OnboardingProfileResponse(
                        o.getNickname(), o.getAgeGroup(), o.getGender(), o.getOccupation(),
                        o.getGoals(), o.getCopingMechanisms(), o.getPrimaryConcerns(), o.getFeelingToday()
                ))
                .orElse(new OnboardingProfileResponse("", "", "", "", "", "", "", ""));
    }

    @Transactional
    public void submitOnboarding(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        UserOnboarding onboarding = onboardingRepository.findByUserId(userId).orElse(new UserOnboarding());

        onboarding.setUser(user);
        onboarding.setNickname(request.nickname());
        onboarding.setAgeGroup(request.ageGroup());
        onboarding.setGender(request.gender());
        onboarding.setCountry(request.country());
        onboarding.setOccupation(request.occupation());
        onboarding.setOccupationDetails(request.occupationDetails());
        onboarding.setFeelingToday(request.feelingToday());
        onboarding.setPrimaryConcerns(request.primaryConcerns() != null ? String.join(", ", request.primaryConcerns()) : "");
        onboarding.setCopingMechanisms(request.copingMechanisms() != null ? String.join(", ", request.copingMechanisms()) : "");
        onboarding.setGoals(request.goals() != null ? String.join(", ", request.goals()) : "");
        onboarding.setOverwhelmedScale(request.overwhelmedScale());
        onboarding.setOverwhelmedFrequency(request.overwhelmedFrequency());
        onboarding.setSafetyRisk(request.safetyRisk());
        onboarding.setSleepQuality(request.sleepQuality());
        onboarding.setChatPreference(request.chatPreference());

        onboardingRepository.save(onboarding);
        user.setHasCompletedOnboarding(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserProfile(Long userId, ProfileUpdateRequest request) {
        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No onboarding profile found for this user."));

        if (request.nickname() != null) onboarding.setNickname(request.nickname());
        if (request.ageGroup() != null) onboarding.setAgeGroup(request.ageGroup());
        if (request.gender() != null) onboarding.setGender(request.gender());
        if (request.occupation() != null) onboarding.setOccupation(request.occupation());
        if (request.feelingToday() != null) onboarding.setFeelingToday(request.feelingToday());
        if (request.goal() != null) onboarding.setGoals(request.goal());
        if (request.cope() != null) onboarding.setCopingMechanisms(request.cope());
        if (request.concern() != null) onboarding.setPrimaryConcerns(request.concern());

        onboardingRepository.save(onboarding);
    }
}