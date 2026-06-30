package com.levosoft.microservice.chat.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.service.IncomingChannelEventProducer;
import com.levosoft.microservice.chat.service.channeladapters.ChannelInboundAdapterRegistry;
import com.levosoft.microservice.chat.service.inboundvalidation.InboundValidationService;
import com.levosoft.microservice.chat.service.messagenormalization.MessageNormalizationService;

@RestController
public class ExternalIngressController {

    private final IncomingChannelEventProducer producer;
    private final ChannelInboundAdapterRegistry channelInboundAdapterRegistry;
    private final MessageNormalizationService messageNormalizationService;
    private final InboundValidationService inboundValidationService;

    public ExternalIngressController(IncomingChannelEventProducer producer,
                                     ChannelInboundAdapterRegistry channelInboundAdapterRegistry,
                                     MessageNormalizationService messageNormalizationService,
                                     InboundValidationService inboundValidationService) {
        this.producer = producer;
        this.channelInboundAdapterRegistry = channelInboundAdapterRegistry;
        this.messageNormalizationService = messageNormalizationService;
        this.inboundValidationService = inboundValidationService;
    }

    @PostMapping(path = "/ingress/channel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ingress(@RequestBody ChannelEventPayload payload,
                                          @RequestHeader HttpHeaders headers) {
        try {
            ChannelEventPayload normalized = messageNormalizationService.normalize(payload, headers);
            inboundValidationService.validate(normalized);
            producer.publish(normalized);
            return ResponseEntity.ok("queued");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid_payload");
        }
    }

    @PostMapping(path = "/ingress/whatsapp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ingressWhatsApp(@RequestBody String rawPayload,
                                                   @RequestHeader HttpHeaders headers) {
        try {
            ChannelEventPayload mappedPayload = channelInboundAdapterRegistry.resolve("whatsapp").adapt(rawPayload);
            if (mappedPayload == null) {
                return ResponseEntity.ok("ignored");
            }
            ChannelEventPayload normalized = messageNormalizationService.normalize(mappedPayload, headers);
            inboundValidationService.validate(normalized);
            producer.publish(normalized);
            return ResponseEntity.ok("queued");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid_payload");
        }
    }
}

