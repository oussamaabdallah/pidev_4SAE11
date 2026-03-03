package com.esprit.task.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A task or subtask within a project")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Column
    @Schema(description = "Contract ID (nullable)", example = "1")
    private Long contractId;

    @Column(nullable = false)
    @Schema(description = "Task title", example = "Implement API")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Task description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Task status")
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Task priority")
    private TaskPriority priority;

    @Column
    @Schema(description = "Assignee (freelancer) ID", example = "10")
    private Long assigneeId;

    @Column
    @Schema(description = "Task deadline — used for Planning calendar integration")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Schema(description = "Sort order index")
    private Integer orderIndex;

    @Column
    @Schema(description = "Parent task ID for subtasks")
    private Long parentTaskId;

    @Column
    @Schema(description = "Creator user ID")
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
