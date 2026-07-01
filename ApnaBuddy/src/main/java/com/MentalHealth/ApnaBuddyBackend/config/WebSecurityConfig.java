package com.MentalHealth.ApnaBuddyBackend.config;

import com.MentalHealth.ApnaBuddyBackend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. Enable CORS for mobile/frontend requests
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Disable CSRF (not needed for stateless JWT APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Make session stateless (no server-side sessions, only JWTs)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Secure the routes
                .authorizeHttpRequests(auth -> auth
                        // Allow anyone to access the Google Auth endpoint
                        .requestMatchers("/api/auth/**").permitAll()

                        // Require a valid JWT for any chat or AI interactions
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/admin/**").permitAll() // Keeping open for your admin tests
                        // Catch-all for anything else
                        .anyRequest().authenticated()
                )

                // 5. Add your custom JWT filter BEFORE Spring's standard authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    // Explicit CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Explicitly allow the Expo web dev server and the Vercel production frontend
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8081", 
                "http://localhost:19006", 
                "https://apna-buddy-frontend.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Required for web browsers making cross-origin requests with tokens
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}