package com.levosoft.microservice.chat.service.channeladapters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.dto.WhatsAppTextMessage;
import com.levosoft.microservice.chat.dto.WhatsAppWebhookPayload;
import com.levosoft.microservice.chat.model.ChannelSourceType;
import com.levosoft.microservice.chat.service.TenantIdentityResolver;

@Service
public class WhatsAppChannelInboundAdapter implements ChannelInboundAdapter {

    private final ObjectMapper objectMapper;
    private final TenantIdentityResolver tenantIdentityResolver;

    public WhatsAppChannelInboundAdapter(ObjectMapper objectMapper,
                                         TenantIdentityResolver tenantIdentityResolver) {
        this.objectMapper = objectMapper;
        this.tenantIdentityResolver = tenantIdentityResolver;
    }

    @Override
    public String channel() {
        return "whatsapp";
    }

    @Override
    public ChannelEventPayload adapt(String rawPayload) {
        try {
            WhatsAppWebhookPayload payload = objectMapper.readValue(rawPayload, WhatsAppWebhookPayload.class);
            if (payload == null || payload.entry() == null || payload.entry().isEmpty()) {
                return null;
            }

            List<WhatsAppWebhookPayload.Change> changes = payload.entry().get(0).changes();
            if (changes == null || changes.isEmpty()) {
                return null;
            }

            WhatsAppWebhookPayload.Value value = changes.get(0).value();
            if (value == null || value.messages() == null || value.messages().isEmpty()) {
                return null;
            }

            WhatsAppTextMessage message = value.messages().get(0);
            String customerIdentity = message.from();
            String conversationPayload = message.text() == null ? null : message.text().body();
            String phoneNumberId = value.metadata() == null ? null : value.metadata().phoneNumberId();

            Map<String, String> metadata = extractMetadata(value, message);
            String tenantIdentity = resolveTenantIdentity(phoneNumberId, metadata);

            if (!StringUtils.hasText(customerIdentity)
                    || !StringUtils.hasText(conversationPayload)
                    || !StringUtils.hasText(tenantIdentity)) {
                return null;
            }

            return new ChannelEventPayload(
                    tenantIdentity.trim(),
                    customerIdentity.trim(),
                    conversationPayload.trim(),
                    ChannelSourceType.INTEGRATION,
                    metadata
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveTenantIdentity(String phoneNumberId, Map<String, String> metadata) {
        if (!StringUtils.hasText(phoneNumberId)) {
            return null;
        }
        String tenantIdentity = phoneNumberId.trim();
        tenantIdentityResolver.resolve(tenantIdentity).ifPresent(resolved -> {
            metadata.put("resolved_tenant_id", String.valueOf(resolved.tenantId()));
            metadata.put("resolved_business_id", String.valueOf(resolved.businessId()));
            if (StringUtils.hasText(resolved.businessName())) {
                metadata.put("resolved_business_name", resolved.businessName().trim());
            }
        });
        return tenantIdentity;
    }

    private Map<String, String> extractMetadata(WhatsAppWebhookPayload.Value value, WhatsAppTextMessage message) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source_channel", "whatsapp");

        if (value.metadata() != null) {
            putIfPresent(metadata, "phone_number_id", value.metadata().phoneNumberId());
            putIfPresent(metadata, "display_phone_number", value.metadata().displayPhoneNumber());
        }

        putIfPresent(metadata, "message_id", message.id());
        putIfPresent(metadata, "timestamp", message.timestamp());

        if (value.contacts() != null && !value.contacts().isEmpty() && value.contacts().get(0).profile() != null) {
            putIfPresent(metadata, "customer_name", value.contacts().get(0).profile().name());
            putIfPresent(metadata, "wa_id", value.contacts().get(0).waId());
        }

        return metadata;
    }

    private void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value.trim());
        }
    }
}

