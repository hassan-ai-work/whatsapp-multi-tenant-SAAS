package com.levosoft.microservice.brain.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Tenant registered channel response payload")
public record ChannelRegistrationResponse(
        @Schema(description = "Unique auto-incremented registration identifier", example = "1")
        Long id,

        @Schema(description = "Owning business identifier", example = "10")
        Long businessId,

        @Schema(description = "Channel lookup code", example = "whatsapp")
        String channelCode,

        @Schema(description = "Channel display name from lookup table", example = "WhatsApp")
        String channelName,

        @Schema(description = "Display name for this registration", example = "Main WhatsApp Line")
        String displayName,

        @Schema(description = "Current registration link status", example = "true")
        Boolean linkedStatus,

        @Schema(description = "Timestamp when the registration row was created")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the registration row was last modified")
        OffsetDateTime updatedAt
) {
}
