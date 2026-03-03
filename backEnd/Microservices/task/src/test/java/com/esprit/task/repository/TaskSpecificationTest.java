package com.esprit.task.repository;

import com.esprit.task.entity.Task;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for TaskSpecification. Verifies that the specification can be built.
 */
class TaskSpecificationTest {

    @Test
    void filtered_withProjectId_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_withSearch_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("test"),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }

    @Test
    void filtered_emptyOptional_returnsNonNullSpec() {
        Specification<Task> spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        assertThat(spec).isNotNull();
    }
}
