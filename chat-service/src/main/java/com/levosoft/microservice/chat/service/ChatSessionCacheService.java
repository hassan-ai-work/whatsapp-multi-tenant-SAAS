package com.levosoft.microservice.chat.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.levosoft.microservice.chat.config.ChatProperties;

@Service
public class ChatSessionCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatProperties chatProperties;

    public ChatSessionCacheService(RedisTemplate<String, String> redisTemplate, ChatProperties chatProperties) {
        this.redisTemplate = redisTemplate;
        this.chatProperties = chatProperties;
    }

    public static String sessionKey(String tenantIdentity, String customerIdentity) {
        return "session:" + tenantIdentity + ":" + customerIdentity;
    }

    public static String snapshotKey(String tenantIdentity, String customerIdentity) {
        return "session-snapshot:" + tenantIdentity + ":" + customerIdentity;
    }

    public String readHistory(String tenantIdentity, String customerIdentity) {
        String key = sessionKey(tenantIdentity, customerIdentity);
        String history = redisTemplate.opsForValue().get(key);
        refreshTtl(key, chatProperties.session().ttl());
        return history == null ? "" : history;
    }

    public void appendInteraction(String tenantIdentity, String customerIdentity, String userMessage, String aiMessage) {
        String key = sessionKey(tenantIdentity, customerIdentity);
        String snapshot = snapshotKey(tenantIdentity, customerIdentity);
        String current = redisTemplate.opsForValue().get(key);
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(current)) {
            builder.append(current.trim()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        builder.append("USER: ").append(userMessage).append(System.lineSeparator());
        builder.append("AI: ").append(aiMessage).append(System.lineSeparator());
        String value = builder.toString().trim();
        redisTemplate.opsForValue().set(key, value, chatProperties.session().ttl());
        redisTemplate.opsForValue().set(snapshot, value, chatProperties.session().snapshotTtl());
    }

    public String readSnapshot(String tenantIdentity, String customerIdentity) {
        String snapshot = snapshotKey(tenantIdentity, customerIdentity);
        String value = redisTemplate.opsForValue().get(snapshot);
        refreshTtl(snapshot, chatProperties.session().snapshotTtl());
        return value == null ? "" : value;
    }

    public void deleteSnapshot(String tenantIdentity, String customerIdentity) {
        redisTemplate.delete(snapshotKey(tenantIdentity, customerIdentity));
    }

    private void refreshTtl(String key, Duration ttl) {
        if (ttl != null && !ttl.isZero() && !ttl.isNegative() && Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, ttl);
        }
    }
}

