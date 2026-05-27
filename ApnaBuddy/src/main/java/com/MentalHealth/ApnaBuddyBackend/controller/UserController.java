package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.OnboardingProfileResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.OnboardingRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.ProfileUpdateRequest;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthUtil authUtil;

    @PostMapping("/onboarding")
    public ResponseEntity<String> submitOnboarding(@Valid @RequestBody OnboardingRequest request) {
        Long userId = authUtil.getCurrentUserId();
        userService.submitOnboarding(userId, request);

        if (request.safetyRisk() != null && request.safetyRisk()) {
            return ResponseEntity.ok("Onboarding saved. CRITICAL_SAFETY_FLAG: Frontend please display helplines.");
        }
        return ResponseEntity.ok("User onboarded successfully.");
    }

    // --- NEW: Fetch Profile Data ---
    @GetMapping("/profile")
    public ResponseEntity<OnboardingProfileResponse> getProfileDetails() {
        Long userId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.getOnboardingProfile(userId));
    }

    // --- Existing: Update Profile Data ---
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Long userId = authUtil.getCurrentUserId();
        userService.updateUserProfile(userId, request);
        return ResponseEntity.ok("Profile updated successfully in database.");
    }
}