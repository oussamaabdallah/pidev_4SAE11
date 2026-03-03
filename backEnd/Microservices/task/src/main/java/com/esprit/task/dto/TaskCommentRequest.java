package com.esprit.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating or updating a task comment")
public class TaskCommentRequest {

    @Schema(description = "Task ID", required = true)
    private Long taskId;

    @Schema(description = "Commenter user ID", required = true)
    private Long userId;

    @Schema(description = "Comment message", required = true)
    private String message;
}
