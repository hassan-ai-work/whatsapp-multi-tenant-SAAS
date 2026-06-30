package com.levosoft.microservice.chat.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

@Component
public class LangChainReplyGateway {

    private final ChatLanguageModel chatLanguageModel;

    public LangChainReplyGateway(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public String generate(String prompt) {
        return chatLanguageModel.generate(prompt);
    }
}

