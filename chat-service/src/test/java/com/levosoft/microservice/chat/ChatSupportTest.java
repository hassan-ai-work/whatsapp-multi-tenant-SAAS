package com.levosoft.microservice.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.levosoft.microservice.chat.model.ChannelSourceType;
import com.levosoft.microservice.chat.service.ChatSessionCacheService;

class ChatSupportTest {

    @Test
    void sessionKeyUsesCompositeSchema() {
        assertThat(ChatSessionCacheService.sessionKey("tenant-a", "customer-b"))
                .isEqualTo("session:tenant-a:customer-b");
    }

    @Test
    void sourceTypeFallbackIsUnknown() {
        assertThat(ChannelSourceType.fromValue("missing")).isEqualTo(ChannelSourceType.UNKNOWN);
    }
}

