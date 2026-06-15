package com.levosoft.microservice.brain.document.controller;

import com.levosoft.microservice.brain.exception.ApiErrorResponse;
import com.levosoft.microservice.brain.exception.GlobalExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentIngestionControllerValidationTest {

    private final TestableGlobalExceptionHandler handler = new TestableGlobalExceptionHandler();

    @Test
    void shouldReturnFieldErrorWhenConstraintViolationHappens() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("ingestDocument.title");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("Title is required");

        ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(violation))
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Title is required", response.getBody().errors().get("title"));
    }

    @Test
    void shouldReturnStructuredBadRequestWhenServiceThrowsResponseStatusException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required multipart field 'file' must not be empty")
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Required multipart field 'file' must not be empty", response.getBody().errors().get("message"));
    }

    @Test
    void shouldReturnStructuredBadRequestWhenMultipartFilePartMissing() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMissingPart(
                new MissingServletRequestPartException("file")
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("is required", response.getBody().errors().get("file"));
    }

    @Test
    void shouldReturnStructuredBadRequestWhenRequiredHeaderMissing() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMissingHeader(
                new MissingRequestHeaderException("X-Authenticated-User", null)
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("is required", response.getBody().errors().get("X-Authenticated-User"));
    }

    private static class TestableGlobalExceptionHandler extends GlobalExceptionHandler {
        ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
            ResponseEntity<Object> response = handleMissingServletRequestPart(
                    ex,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    new ServletWebRequest(mock(HttpServletRequest.class))
            );
            return cast(response);
        }

        ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
            ResponseEntity<Object> response = handleServletRequestBindingException(
                    ex,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    new ServletWebRequest(mock(HttpServletRequest.class))
            );
            return cast(response);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<ApiErrorResponse> cast(ResponseEntity<Object> response) {
            return (ResponseEntity<ApiErrorResponse>) (ResponseEntity<?>) response;
        }
    }
}

