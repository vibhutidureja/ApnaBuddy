//package com.codingshuttle.learn_spring_ai.service;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.TimeZone;
//
//@SpringBootTest(properties = {
//        "spring.ai.vectorstore.pgvector.enabled=false"
//})
//public class AIServiceTests {
//
//    @Autowired
//    private RAGService rAGService;
//
//    @BeforeAll
//    static void setTimezone() {
//        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
//    }
//
//    @Autowired
//    private AIService aiService;
//
//    @Test
//    public void ingest() {
//        aiService.ingestCounselChat();
//        aiService.ingestEmpathetic();
//    }
//
//    @Test
//    public void response() {
//
////        rAGService.ingestPDFtoVectorStore(); // 🔥 MUST
//
//        String result = rAGService.askai("I dont feel like to do anything now a days");
//
//        System.out.println("AI Response:\n" + result);
//    }
//    @Test
//    public void response2() {
//        String result=rAGService.askaiwithAdvisors("whats my name","Parth");
//        System.out.println("AI Response:\n" + result);
//
//    }
//}