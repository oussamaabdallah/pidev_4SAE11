package com.esprit.task.dto;

import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating or updating a task")
public class TaskRequest {

    @Schema(description = "Project ID", required = true)
    private Long projectId;

    @Schema(description = "Contract ID")
    private Long contractId;

    @Schema(description = "Task title", required = true)
    private String title;

    @Schema(description = "Task description")
    private String description;

    @Schema(description = "Task status")
    private TaskStatus status;

    @Schema(description = "Task priority")
    private TaskPriority priority;

    @Schema(description = "Assignee (freelancer) ID")
    private Long assigneeId;

    @Schema(description = "Task deadline for calendar integration")
    private LocalDate dueDate;

    @Schema(description = "Sort order index")
    private Integer orderIndex;

    @Schema(description = "Parent task ID for subtasks")
    private Long parentTaskId;

    @Schema(description = "Creator user ID")
    private Long createdBy;
}
