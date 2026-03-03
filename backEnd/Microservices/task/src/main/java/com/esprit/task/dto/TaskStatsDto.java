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
@Schema(description = "Task statistics for a project or freelancer")
public class TaskStatsDto {

    @Schema(description = "Total task count")
    private long totalTasks;

    @Schema(description = "Done count")
    private long doneCount;

    @Schema(description = "In progress count")
    private long inProgressCount;

    @Schema(description = "Overdue count")
    private long overdueCount;

    @Schema(description = "Completion percentage (0-100)")
    private double completionPercentage;
}
