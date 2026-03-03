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
    void handleEntityNotFound_returns404WithMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("ProgressUpdate", 999L);

        ResponseEntity<Map<String, String>> result = handler.handleEntityNotFound(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).containsKey("message");
        assertThat(result.getBody().get("message")).contains("ProgressUpdate not found");
        assertThat(result.getBody().get("message")).contains("999");
    }
}
