package com.MentalHealth.ApnaBuddyBackend.controller;

import com.MentalHealth.ApnaBuddyBackend.service.GlobalDataIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GlobalDataIngestionService ingestionService;

    // Make sure to hit this endpoint only ONCE after setting up a fresh database!
    // POST http://localhost:8084/api/admin/ingest-global-data
    @PostMapping("/ingest-global-data")
    public ResponseEntity<String> ingestData() {
        ingestionService.ingestAllGlobalData();
        return ResponseEntity.ok("Global data ingestion started. Check server logs for progress.");
    }
}