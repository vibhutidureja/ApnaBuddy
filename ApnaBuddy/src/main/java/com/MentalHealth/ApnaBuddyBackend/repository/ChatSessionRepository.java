package com.MentalHealth.ApnaBuddyBackend.repository;

import com.MentalHealth.ApnaBuddyBackend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // Used for the app history list: Only show sessions that haven't been soft-deleted
    List<ChatSession> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    // Used to find the current active session for the user
    Optional<ChatSession> findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    // Security check
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}