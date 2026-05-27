package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.AuthResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.GoogleLoginRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.UserProfileResponse;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.AuthService;
import com.MentalHealth.ApnaBuddyBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthUtil authUtil;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        // The service will verify the Google token, create the user if they don't exist,
        // and return your custom backend JWT.
        return ResponseEntity.ok(authService.googleLogin(request.idToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        // Securely grab the ID of the user making the request via their JWT
        Long currentUserId = authUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(currentUserId));
    }
}