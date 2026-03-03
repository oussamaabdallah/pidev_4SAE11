package com.esprit.task.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionTest {

    @Test
    void constructor_withEntityAndId_setsMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("Task", 999L);

        assertThat(ex.getMessage()).contains("Task").contains("999");
    }

    @Test
    void constructor_withMessageOnly_setsMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("Custom message");

        assertThat(ex.getMessage()).isEqualTo("Custom message");
    }
}
