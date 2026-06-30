package com.levosoft.microservice.chat.service.channeladapters;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;

public interface ChannelInboundAdapter {

    String channel();

    ChannelEventPayload adapt(String rawPayload);
}

