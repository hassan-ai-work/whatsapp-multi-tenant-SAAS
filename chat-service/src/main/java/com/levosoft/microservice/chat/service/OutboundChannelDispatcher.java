package com.levosoft.microservice.chat.service;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.dto.OutboundChannelRequest;

@Service
public class OutboundChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboundChannelDispatcher.class);

    private final RestClient restClient;
    private final ChatProperties chatProperties;

    public OutboundChannelDispatcher(RestClient restClient, ChatProperties chatProperties) {
        this.restClient = restClient;
        this.chatProperties = chatProperties;
    }

    public void dispatch(ChannelEventPayload payload, String replyText, String sessionKey) {
        OutboundChannelRequest request = new OutboundChannelRequest(
                payload.tenantIdentity(),
                payload.customerIdentity(),
                payload.channelSourceType(),
                replyText,
                sessionKey,
                Instant.now(),
                payload.metadata());

        log.info("Outbound reply queued tenant={} customer={} endpoint={}",
                payload.tenantIdentity(), payload.customerIdentity(), chatProperties.outbound().endpoint());

        restClient.post()
                .uri(chatProperties.outbound().endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}

