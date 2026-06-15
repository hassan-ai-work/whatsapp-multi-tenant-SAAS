package com.levosoft.microservice.brain.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelRegistrationRequest(
        @NotBlank(message = "channelCode is required")
        @Size(min = 2, max = 100, message = "channelCode must be between 2 and 100 characters")
        @Schema(description = "Lookup code of the channel", example = "whatsapp", requiredMode = Schema.RequiredMode.REQUIRED)
        String channelCode,

        @NotBlank(message = "displayName is required")
        @Size(min = 1, max = 255, message = "displayName must be between 1 and 255 characters")
        @Schema(description = "Display name for this channel registration", example = "Main WhatsApp Line", requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName
) {
}
