package com.levosoft.microservice.chat.config;

import java.util.Locale;

import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.levosoft.microservice.chat.service.FallbackEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

@Configuration
public class LangChainConfiguration {

    @Bean
    public ChatLanguageModel chatLanguageModel(ChatProperties chatProperties) {
        String provider = chatProperties.llm().provider().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(chatProperties.llm().baseUrl())
                    .modelName(chatProperties.llm().model())
                    .build();
            case "openai" -> OpenAiChatModel.builder()
                    .apiKey(chatProperties.llm().apiKey())
                    .modelName(chatProperties.llm().model())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported chat provider: " + chatProperties.llm().provider());
        };
    }

    @Bean
    public EmbeddingModel embeddingModel(ChatProperties chatProperties) {
        if (!StringUtils.hasText(chatProperties.embedding().apiKey())) {
            return new FallbackEmbeddingModel(chatProperties.embedding().dimensions());
        }
        String provider = chatProperties.embedding().provider().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "openai" -> OpenAiEmbeddingModel.builder()
                    .apiKey(chatProperties.embedding().apiKey())
                    .baseUrl(chatProperties.embedding().baseUrl())
                    .modelName(chatProperties.embedding().model())
                    .dimensions(chatProperties.embedding().dimensions())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unsupported embedding provider: " + chatProperties.embedding().provider()
                            + ". Use a 1536-dim embedding model.");
        };
    }
}

