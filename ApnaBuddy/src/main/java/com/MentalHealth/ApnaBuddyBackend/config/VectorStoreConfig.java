package com.MentalHealth.ApnaBuddyBackend.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    // 1. The Global Dataset Table (PDFs, CSVs)
    @Bean(name = "globalVectorStore")
    public VectorStore globalVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("global_vector_store") // Dropped "with"
                .dimensions(1536)                       // Dropped "with"
                .initializeSchema(true)                 // Dropped "with"
                .build();
    }

    // 2. The User History Table (Chat Memories)
    @Bean(name = "userVectorStore")
    @Primary
    public VectorStore userVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("user_vector_store")   // Dropped "with"
                .dimensions(1536)                       // Dropped "with"
                .initializeSchema(true)                 // Dropped "with"
                .build();
    }
}