package com.levosoft.microservice.chat.service.channeladapters;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ChannelInboundAdapterRegistry {

    private final Map<String, ChannelInboundAdapter> adapters;

    public ChannelInboundAdapterRegistry(List<ChannelInboundAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toUnmodifiableMap(a -> normalize(a.channel()), Function.identity()));
    }

    public ChannelInboundAdapter resolve(String channel) {
        ChannelInboundAdapter adapter = adapters.get(normalize(channel));
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported channel adapter: " + channel);
        }
        return adapter;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

