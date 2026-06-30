package com.levosoft.microservice.chat.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.model.DocumentContextChunk;
import com.levosoft.microservice.chat.service.inboundvalidation.InboundValidationService;

@Service
public class ConversationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationOrchestrationService.class);
    private static final String UNAVAILABLE_REPLY =
            "Sorry, information unavailable right now. I cannot confirm that from current documents.";

    private final ChatSessionCacheService chatSessionCacheService;
    private final EmbeddingGateway embeddingGateway;
    private final VectorStoreLookup vectorStoreLookup;
    private final PromptComposer promptComposer;
    private final LangChainReplyGateway replyGateway;
    private final OutboundChannelDispatcher outboundChannelDispatcher;
    private final ChatProperties chatProperties;
    private final InboundValidationService inboundValidationService;

    public ConversationOrchestrationService(ChatSessionCacheService chatSessionCacheService,
                                            EmbeddingGateway embeddingGateway,
                                            VectorStoreLookup vectorStoreLookup,
                                            PromptComposer promptComposer,
                                            LangChainReplyGateway replyGateway,
                                            OutboundChannelDispatcher outboundChannelDispatcher,
                                            ChatProperties chatProperties,
                                            InboundValidationService inboundValidationService) {
        this.chatSessionCacheService = chatSessionCacheService;
        this.embeddingGateway = embeddingGateway;
        this.vectorStoreLookup = vectorStoreLookup;
        this.promptComposer = promptComposer;
        this.replyGateway = replyGateway;
        this.outboundChannelDispatcher = outboundChannelDispatcher;
        this.chatProperties = chatProperties;
        this.inboundValidationService = inboundValidationService;
    }

    public void process(ChannelEventPayload payload) {
        inboundValidationService.validate(payload);
        String sessionKey = ChatSessionCacheService.sessionKey(payload.tenantIdentity(), payload.customerIdentity());
        String history = chatSessionCacheService.readHistory(payload.tenantIdentity(), payload.customerIdentity());
        float[] queryVector = embeddingGateway.embed(payload.conversationPayload());
        List<DocumentContextChunk> chunks = vectorStoreLookup.lookup(
                payload.tenantIdentity(),
                queryVector,
                chatProperties.rag().maxResults());

        String replyText;
        if (insufficientEvidence(payload.conversationPayload(), chunks)) {
            replyText = UNAVAILABLE_REPLY;
        } else {
            String prompt = promptComposer.compose(payload, history, chunks);
            replyText = replyGateway.generate(prompt);
            if (!StringUtils.hasText(replyText)) {
                replyText = UNAVAILABLE_REPLY;
            }
        }

        chatSessionCacheService.appendInteraction(
                payload.tenantIdentity(),
                payload.customerIdentity(),
                payload.conversationPayload(),
                replyText);
        outboundChannelDispatcher.dispatch(payload, replyText, sessionKey);

        log.info("Conversation processed tenant={} customer={} session={}",
                payload.tenantIdentity(), payload.customerIdentity(), sessionKey);
    }


    private boolean insufficientEvidence(String message, List<DocumentContextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return true;
        }
        double bestScore = chunks.stream().mapToDouble(DocumentContextChunk::score).max().orElse(0.0d);
        if (bestScore < chatProperties.rag().minimumScore()) {
            return true;
        }

        String normalizedMessage = normalize(message);
        boolean overlap = chunks.stream()
                .map(DocumentContextChunk::content)
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .anyMatch(content -> containsSharedToken(normalizedMessage, content));
        return !overlap;
    }

    private boolean containsSharedToken(String message, String content) {
        String[] tokens = message.split("\\s+");
        for (String token : tokens) {
            if (token.length() > 3 && content.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
    }
}

