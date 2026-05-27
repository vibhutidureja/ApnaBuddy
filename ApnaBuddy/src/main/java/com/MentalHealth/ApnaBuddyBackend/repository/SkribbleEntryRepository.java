package com.MentalHealth.ApnaBuddyBackend.repository;

import com.MentalHealth.ApnaBuddyBackend.entity.SkribbleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkribbleEntryRepository extends JpaRepository<SkribbleEntry, Long> {
    List<SkribbleEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
}