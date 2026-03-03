package com.esprit.task.controller;

import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task Stats", description = "Task statistics by project, freelancer, and dashboard")
public class TaskStatsController {

    private final TaskService taskService;

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Stats by project", description = "Returns task counts and completion percentage for a project.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsDto> getByProject(@Parameter(description = "Project ID", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getStatsByProject(projectId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    @Operation(summary = "Stats by freelancer", description = "Returns task stats for the given assignee, with optional date range.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsDto> getByFreelancer(
            @Parameter(description = "Freelancer ID", required = true) @PathVariable Long freelancerId,
            @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(taskService.getStatsByFreelancer(freelancerId, Optional.ofNullable(from), Optional.ofNullable(to)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats", description = "Returns global task counts.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsDto> getDashboard() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }
}
