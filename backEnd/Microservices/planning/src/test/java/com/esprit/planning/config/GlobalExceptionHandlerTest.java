package com.esprit.planning.config;

import com.esprit.planning.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlobalExceptionHandler. Verifies EntityNotFoundException maps to 404.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404WithErrorEnvelope() {
        EntityNotFoundException ex = new EntityNotFoundException("ProgressUpdate", 999L);

        ResponseEntity<Map<String, Object>> result = handler.handleEntityNotFound(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).containsKey("error");
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.getBody().get("error");
        assertThat(error).containsEntry("code", "NOT_FOUND");
        assertThat(error.get("message")).asString().contains("ProgressUpdate not found");
        assertThat(error.get("message")).asString().contains("999");
    }
}
