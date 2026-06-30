package com.levosoft.microservice.chat.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ChannelSourceType {
    MOBILE,
    WEB,
    INTEGRATION,
    UNKNOWN;

    @JsonCreator
    public static ChannelSourceType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ChannelSourceType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

