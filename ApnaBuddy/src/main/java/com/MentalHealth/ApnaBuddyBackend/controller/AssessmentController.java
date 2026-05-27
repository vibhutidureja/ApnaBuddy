package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.dto.AssessmentDtos.AssessmentReport;
import com.MentalHealth.ApnaBuddyBackend.dto.AssessmentDtos.AssessmentSubmitRequest;
import com.MentalHealth.ApnaBuddyBackend.dto.AssessmentDtos.QuestionDto;
import com.MentalHealth.ApnaBuddyBackend.security.AuthUtil;
import com.MentalHealth.ApnaBuddyBackend.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuthUtil authUtil;

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDto>> getQuestions() {
        return ResponseEntity.ok(assessmentService.getAllQuestions());
    }

    @PostMapping("/submit")
    public ResponseEntity<AssessmentReport> submitAssessment(
            @Valid @RequestBody AssessmentSubmitRequest request) {

        Long currentUserId = authUtil.getCurrentUserId();
        AssessmentReport report = assessmentService.submitAssessment(currentUserId, request);
        return ResponseEntity.ok(report);
    }
}