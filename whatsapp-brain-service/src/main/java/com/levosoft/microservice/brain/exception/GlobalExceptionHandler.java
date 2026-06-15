package com.levosoft.microservice.brain.exception;

import tools.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";
        errors.put("message", message);
        return buildErrorResponse(ex.getStatusCode(), errors);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(extractLeafFieldName(violation.getPropertyPath().toString()), violation.getMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        Throwable rootCause = ex.getCause();

        if (rootCause instanceof tools.jackson.databind.exc.InvalidFormatException ife) {
            Class<?> targetType = ife.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
                String allowedValues = java.util.Arrays.toString(targetType.getEnumConstants());
                String fieldName = extractFieldNameFromInvalidFormat(ife);
                errors.put(fieldName, String.format("Invalid value '%s'. Allowed values are: %s", invalidValue, allowedValues));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
            }
        }

        errors.put("message", "Malformed JSON request body or primitive type mismatch");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getRequestPartName(), "is required");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            org.springframework.web.bind.ServletRequestBindingException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (ex instanceof org.springframework.web.bind.MissingRequestHeaderException missingHeaderException) {
            errors.put(missingHeaderException.getHeaderName(), "is required");
        } else {
            errors.put("message", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "is required");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorBody(HttpStatus.BAD_REQUEST, errors));
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatusCode statusCode, Map<String, String> errors) {
        ApiErrorResponse body = buildErrorBody(statusCode, errors);
        return new ResponseEntity<>(body, statusCode);
    }

    private ApiErrorResponse buildErrorBody(HttpStatusCode statusCode, Map<String, String> errors) {
        HttpStatus resolvedStatus = HttpStatus.resolve(statusCode.value());
        String reason = resolvedStatus != null ? resolvedStatus.getReasonPhrase() : "Error";
        return new ApiErrorResponse(reason, errors, Instant.now(), statusCode.value());
    }

    private String extractLeafFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) {
            return "field";
        }
        int lastDot = propertyPath.lastIndexOf('.');
        if (lastDot < 0 || lastDot == propertyPath.length() - 1) {
            return propertyPath;
        }
        return propertyPath.substring(lastDot + 1);
    }

    private String extractFieldNameFromInvalidFormat(InvalidFormatException ife) {
        String pathReference = ife.getPathReference();
        if (pathReference == null || pathReference.isBlank()) {
            return "field";
        }

        int lastQuote = pathReference.lastIndexOf('"');
        if (lastQuote <= 0) {
            return "field";
        }
        int previousQuote = pathReference.lastIndexOf('"', lastQuote - 1);
        if (previousQuote < 0 || previousQuote == lastQuote - 1) {
            return "field";
        }
        return pathReference.substring(previousQuote + 1, lastQuote);
    }

}

