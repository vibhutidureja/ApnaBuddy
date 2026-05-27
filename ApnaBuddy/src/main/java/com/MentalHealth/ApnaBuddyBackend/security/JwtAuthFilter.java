package com.MentalHealth.ApnaBuddyBackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestHeaderToken = request.getHeader("Authorization");

        // 1. Only attempt parsing if the header actually exists and starts with Bearer
        if (requestHeaderToken != null && requestHeaderToken.startsWith("Bearer ")) {
            String jwtToken = requestHeaderToken.substring(7);

            try {
                // Verify and extract user details
                JwtUserPrincipal user = authUtil.verifyAccessToken(jwtToken);

                // If valid and not already authenticated in this thread
                if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            user, null, user.authorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception e) {
                // 2. If the token is garbage or expired, just log it.
                // We DO NOT crash the request. The user simply remains "Unauthenticated".
                log.warn("JWT Token invalid or expired: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 3. ALWAYS continue the filter chain OUTSIDE the try-catch.
        // If the route is /api/auth/google, WebSecurityConfig permits it anyway.
        // If the route is /api/chat, WebSecurityConfig blocks it because they aren't authenticated.
        filterChain.doFilter(request, response);
    }
}