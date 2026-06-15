package com.levosoft.microservice.brain.exception;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String error,
        Map<String, String> errors,
        Instant timestamp,
        int status
) {
}


