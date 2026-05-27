package com.MentalHealth.ApnaBuddyBackend.repository;

import com.MentalHealth.ApnaBuddyBackend.entity.AssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecord, Long> {

    // Add this line for your report generation (Chronological order)
    List<AssessmentRecord> findByUserIdOrderByCreatedAtAsc(Long userId);

    // You likely already have this one for fetching the latest assessment
    List<AssessmentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}