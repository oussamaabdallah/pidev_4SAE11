package com.esprit.task.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A comment on a task")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Task ID", example = "1")
    private Long taskId;

    @Column(nullable = false)
    @Schema(description = "Commenter user ID", example = "10")
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Schema(description = "Comment message")
    private String message;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
