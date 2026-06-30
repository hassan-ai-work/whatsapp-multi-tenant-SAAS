package com.levosoft.microservice.chat.listener;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.levosoft.microservice.chat.service.ChatSessionCacheService;
import com.levosoft.microservice.chat.service.ContactMemoryArchiveService;

@Component
public class SessionExpiryListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(SessionExpiryListener.class);

    private final ChatSessionCacheService chatSessionCacheService;
    private final ContactMemoryArchiveService contactMemoryArchiveService;

    public SessionExpiryListener(ChatSessionCacheService chatSessionCacheService,
                                 ContactMemoryArchiveService contactMemoryArchiveService) {
        this.chatSessionCacheService = chatSessionCacheService;
        this.contactMemoryArchiveService = contactMemoryArchiveService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message == null ? null : new String(message.getBody(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(expiredKey) || !expiredKey.startsWith("session:")) {
            return;
        }

        String[] parts = expiredKey.split(":", 3);
        if (parts.length != 3) {
            log.warn("Expired key ignored. Unexpected format: {}", expiredKey);
            return;
        }

        String tenantIdentity = parts[1];
        String customerIdentity = parts[2];
        String transcript = chatSessionCacheService.readSnapshot(tenantIdentity, customerIdentity);
        if (!StringUtils.hasText(transcript)) {
            log.warn("No snapshot transcript found for expired key {}", expiredKey);
            return;
        }

        contactMemoryArchiveService.archive(tenantIdentity, customerIdentity, transcript, expiredKey);
        chatSessionCacheService.deleteSnapshot(tenantIdentity, customerIdentity);
        log.info("Archived expired session {}", expiredKey);
    }
}

