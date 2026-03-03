package com.esprit.task.dto;

import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kanban board representation: tasks grouped by status")
public class TaskBoardDto {

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Tasks grouped by status (TODO, IN_PROGRESS, IN_REVIEW, DONE)")
    private Map<TaskStatus, List<Task>> columns;
}
