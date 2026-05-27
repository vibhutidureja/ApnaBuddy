package com.MentalHealth.ApnaBuddyBackend.repository;

import com.MentalHealth.ApnaBuddyBackend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Add this line so your ReportService can fetch all activities for a user
    List<ActivityLog> findByUserId(Long userId);

    // You likely already have this one from our earlier ChatService updates
    Optional<ActivityLog> findFirstByUserIdOrderByCompletedAtDesc(Long userId);
}