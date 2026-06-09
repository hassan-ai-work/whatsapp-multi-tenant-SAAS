package com.levosoft.microservice.brain.exception;

import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ((HttpStatus) ex.getStatusCode()).getReasonPhrase());
        body.put("message", ex.getReason());

        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("timestamp", java.time.Instant.now());
        body.put("status", org.springframework.http.HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        Throwable rootCause = ex.getCause();

        // Dynamically catch the new Jackson 3.x InvalidFormatException
        if (rootCause instanceof tools.jackson.databind.exc.InvalidFormatException ife) {
            Class<?> targetType = ife.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
                String allowedValues = java.util.Arrays.toString(targetType.getEnumConstants());

                // Scalable solution: Works automatically for all enums in your system
                body.put("message", String.format("Invalid value '%s'. Allowed values are: %s", invalidValue, allowedValues));
                return new ResponseEntity<>(body, org.springframework.http.HttpStatus.BAD_REQUEST);
            }
        }

        body.put("message", "Malformed JSON request body or primitive type mismatch");
        return new ResponseEntity<>(body, org.springframework.http.HttpStatus.BAD_REQUEST);
    }

}
