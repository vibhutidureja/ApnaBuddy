package com.MentalHealth.ApnaBuddyBackend.service;

import com.MentalHealth.ApnaBuddyBackend.dto.GameDtos.*;
import com.MentalHealth.ApnaBuddyBackend.entity.SkribbleEntry;
import com.MentalHealth.ApnaBuddyBackend.entity.User;
import com.MentalHealth.ApnaBuddyBackend.repository.SkribbleEntryRepository;
import com.MentalHealth.ApnaBuddyBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final SkribbleEntryRepository skribbleRepo;
    private final UserRepository userRepository;

    @Transactional
    public SkribbleResponse saveSkribble(Long userId, SkribbleRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        SkribbleEntry entry = SkribbleEntry.builder()
                .user(user)
                .prompt(request.prompt())
                .drawingDataBase64(request.drawingDataBase64())
                .build();

        entry = skribbleRepo.save(entry);
        log.info("Skribble game saved for user {}", userId);

        return new SkribbleResponse(
                entry.getId(),
                entry.getPrompt(),
                entry.getDrawingDataBase64(),
                entry.getCreatedAt()
        );

    }
    @Transactional(readOnly = true)
    public List<SkribbleResponse> getUserSkribbles(Long userId) {
        return skribbleRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entry -> new SkribbleResponse(
                        entry.getId(),
                        entry.getPrompt(),
                        entry.getDrawingDataBase64(),
                        entry.getCreatedAt()
                )).toList();
    }
}