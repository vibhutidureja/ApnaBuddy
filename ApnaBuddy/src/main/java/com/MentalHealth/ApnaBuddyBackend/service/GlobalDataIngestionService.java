package com.MentalHealth.ApnaBuddyBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GlobalDataIngestionService {

    // Target the specific Global Vector Store Bean
    private final VectorStore globalVectorStore;

    // Explicit constructor required for @Qualifier
    public GlobalDataIngestionService(@Qualifier("globalVectorStore") VectorStore globalVectorStore) {
        this.globalVectorStore = globalVectorStore;
    }

    // --- ALL PDFs ---
    @Value("classpath:/John_Therapy_Ultra_Elaborative.pdf")
    private Resource pdf1;

    @Value("classpath:/julie_convo.pdf")
    private Resource pdf2;

    @Value("classpath:/rita_convo.pdf")
    private Resource pdf3;

    @Value("classpath:/BB3-Session-2-Annotated-Transcript.pdf")
    private Resource pdf4;

    @Value("classpath:/BB3-Session-10-Annotated-Transcript.pdf")
    private Resource pdf5;

    // --- MAIN EXECUTION METHOD ---
    public void ingestAllGlobalData() {
        log.info("Starting global data ingestion...");
        ingestCounselChat();
        ingestEmpathetic();
        ingestPDFs();
        log.info("✅ All global therapeutic data loaded into the Global Vector Store successfully.");
    }

    // --- 1. COUNSELCHAT DATASET ---
    private void ingestCounselChat() {
        try {
            InputStream is = getClass().getResourceAsStream("/datasets/20220401_counsel_chat.csv");
            if (is == null) {
                log.warn("CounselChat CSV not found!");
                return;
            }

            CSVReader reader = new CSVReader(new InputStreamReader(is));
            List<String[]> rows = reader.readAll();
            List<Document> docs = new ArrayList<>();

            for (int i = 1; i < rows.size(); i++) {
                String question = rows.get(i)[2];
                String answer = rows.get(i)[7];

                if (question == null || answer == null || question.length() < 15) continue;

                String text = "Client: " + question + "\nTherapist: " + answer;

                docs.add(new Document(text, Map.of("source", "counselchat")));
            }
            // Save to global table
            globalVectorStore.add(docs);
            log.info("✅ CounselChat Loaded: {} records", docs.size());
        } catch (Exception e) {
            log.error("Failed to load CounselChat", e);
        }
    }

    // --- 2. EMPATHETIC DATASET ---
    private void ingestEmpathetic() {
        try {
            InputStream is = getClass().getResourceAsStream("/datasets/emotion-emotion_69k.csv");
            if (is == null) {
                log.warn("Empathetic JSON not found!");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(is, List.class);
            List<Document> docs = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String user = (String) row.get("Situation");
                String response = (String) row.get("labels");

                if (user == null || response == null) continue;

                String text = "Client: " + user + "\nTherapist: " + response;

                docs.add(new Document(text, Map.of("source", "empathetic")));
            }
            // Save to global table
            globalVectorStore.add(docs);
            log.info("✅ Empathetic Dataset Loaded: {} records", docs.size());
        } catch (Exception e) {
            log.error("Failed to load Empathetic dataset", e);
        }
    }

    // --- 3. ALL PDFs ---
    private void ingestPDFs() {
        List<Resource> pdfResources = List.of(pdf1, pdf2, pdf3, pdf4, pdf5);
        List<Document> allChunks = new ArrayList<>();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(300)
                .build();

        for (Resource pdf : pdfResources) {
            if (!pdf.exists()) {
                log.warn("PDF not found: {}", pdf.getFilename());
                continue;
            }

            PagePdfDocumentReader reader = new PagePdfDocumentReader(pdf);
            List<Document> documents = reader.get();

            documents.forEach(doc -> {
                doc.getMetadata().put("source", pdf.getFilename());
            });

            allChunks.addAll(splitter.apply(documents));
        }

        if (!allChunks.isEmpty()) {
            // Save to global table
            globalVectorStore.add(allChunks);
            log.info("✅ PDFs Loaded: {} total chunks across {} files", allChunks.size(), pdfResources.size());
        }
    }
}