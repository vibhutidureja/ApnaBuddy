package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.AuthResponse;
import com.MentalHealth.ApnaBuddyBackend.dto.UserProfileResponse;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    // Pulls your Google Client ID from application.yml
    @Value("${app.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse googleLogin(String idTokenString) {
        try {
            // 1. Initialize the Google Token Verifier
            // This ensures the token actually came from Google and is intended for your specific app
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Verify the token securely with Google
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("Invalid Google ID token provided.");
                throw new RuntimeException("Invalid Google ID token.");
            }

            // 3. Extract the user's verified payload from the token
            GoogleIdToken.Payload payload = idToken.getPayload();

            // The 'subject' is Google's unique, permanent ID for this user
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // 4. Find existing user OR create a new one if this is their first time logging in
            User user = userRepository.findByGoogleId(googleId)
                    .orElseGet(() -> {
                        log.info("Creating new user account for email: {}", email);
                        User newUser = User.builder()
                                .googleId(googleId)
                                .email(email)
                                .name(name)
                                .pictureUrl(pictureUrl)
                                .build();
                        return userRepository.save(newUser);
                    });

            // 5. Generate your custom backend JWT using your AuthUtil class
            String accessToken = authUtil.generateAccessToken(user);

// 6. Map the User entity to a safe DTO for the frontend
            UserProfileResponse profile = new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getPictureUrl(),
                    user.getHasCompletedOnboarding(), // <-- ADD THIS
                    user.getHasCompletedAssessment()  // <-- ADD THIS
            );

            // 7. Return the token and profile details to the mobile app
            return new AuthResponse(accessToken, profile);

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
}