package com.levosoft.microservice.chat.service.channeladapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.service.TenantIdentityResolver;

class WhatsAppChannelInboundAdapterTest {

    @Test
    void adaptShouldMapWhatsAppPayloadToCanonicalEvent() {
        TenantIdentityResolver resolver = mock(TenantIdentityResolver.class);
        when(resolver.resolve(anyString())).thenReturn(Optional.empty());

        ObjectMapper objectMapper = JsonMapper.builder().build();
        WhatsAppChannelInboundAdapter adapter = new WhatsAppChannelInboundAdapter(objectMapper, resolver);

        String rawPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "metadata": {
                              "phone_number_id": "tenant-123",
                              "display_phone_number": "1555000111"
                            },
                            "messages": [
                              {
                                "id": "wamid.1",
                                "from": "1555111222",
                                "timestamp": "1719300000",
                                "text": {
                                  "body": "hello from whatsapp"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ChannelEventPayload event = adapter.adapt(rawPayload);

        assertNotNull(event);
        assertEquals("tenant-123", event.tenantIdentity());
        assertEquals("1555111222", event.customerIdentity());
        assertEquals("hello from whatsapp", event.conversationPayload());
        assertEquals("whatsapp", event.metadata().get("source_channel"));
    }
}

