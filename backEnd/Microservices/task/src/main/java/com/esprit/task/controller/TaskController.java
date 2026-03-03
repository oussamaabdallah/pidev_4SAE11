package com.esprit.task.controller;

import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.dto.TaskRequest;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Tasks", description = "CRUD and advanced operations for tasks and subtasks")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Paginated list with filters", description = "Returns paginated tasks with optional filters.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Page<Task>> getFiltered(
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort (e.g. createdAt,desc)") @RequestParam(required = false) String sort,
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "Filter by contract ID") @RequestParam(required = false) Long contractId,
            @Parameter(description = "Filter by assignee ID") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Filter by parent task ID") @RequestParam(required = false) Long parentId,
            @Parameter(description = "Search in title and description") @RequestParam(required = false) String search,
            @Parameter(description = "Due date from (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dueDateFrom,
            @Parameter(description = "Due date to (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dueDateTo) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Task> result = taskService.findAllFiltered(
                Optional.ofNullable(projectId), Optional.ofNullable(contractId), Optional.ofNullable(assigneeId),
                Optional.ofNullable(status), Optional.ofNullable(priority), Optional.ofNullable(parentId),
                Optional.ofNullable(search), Optional.ofNullable(dueDateFrom), Optional.ofNullable(dueDateTo),
                pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Returns a single task by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    public ResponseEntity<Task> getById(@Parameter(description = "Task ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List by project", description = "Returns all tasks for the given project.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<Task>> getByProjectId(@Parameter(description = "Project ID", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.findByProjectId(projectId));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "List by contract", description = "Returns all tasks for the given contract.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<Task>> getByContractId(@Parameter(description = "Contract ID", required = true) @PathVariable Long contractId) {
        return ResponseEntity.ok(taskService.findByContractId(contractId));
    }

    @GetMapping("/assignee/{assigneeId}")
    @Operation(summary = "List by assignee", description = "Returns all tasks assigned to the given freelancer.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<Task>> getByAssigneeId(@Parameter(description = "Assignee ID", required = true) @PathVariable Long assigneeId) {
        return ResponseEntity.ok(taskService.findByAssigneeId(assigneeId));
    }

    @GetMapping("/board/project/{projectId}")
    @Operation(summary = "Kanban board by project", description = "Returns tasks grouped by status for Kanban view.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<?> getBoardByProject(@Parameter(description = "Project ID", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getBoardByProject(projectId));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Overdue tasks", description = "Returns overdue tasks, optionally filtered by project or assignee.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<Task>> getOverdue(
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "Filter by assignee ID") @RequestParam(required = false) Long assigneeId) {
        return ResponseEntity.ok(taskService.getOverdueTasks(Optional.ofNullable(projectId), Optional.ofNullable(assigneeId)));
    }

    @GetMapping("/calendar-events")
    @Operation(summary = "Calendar events", description = "Returns tasks with dueDate in range, for Planning calendar integration. Params: timeMin, timeMax (ISO-8601), userId (optional).")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<TaskCalendarEventDto>> getCalendarEvents(
            @Parameter(description = "Start of range (ISO-8601)") @RequestParam(required = false) String timeMin,
            @Parameter(description = "End of range (ISO-8601)") @RequestParam(required = false) String timeMax,
            @Parameter(description = "Filter to tasks for this user (assignee or project client)") @RequestParam(required = false) Long userId) {
        LocalDateTime min = parseDateTime(timeMin, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        LocalDateTime max = parseDateTime(timeMax, min.plusMonths(2));
        List<TaskCalendarEventDto> events = taskService.getCalendarEvents(min, max, Optional.ofNullable(userId));
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @Operation(summary = "Create task", description = "Creates a new task.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = Task.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    public ResponseEntity<Task> create(@RequestBody TaskRequest request) {
        Task task = toEntity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(task));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates an existing task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = Task.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    public ResponseEntity<Task> update(
            @Parameter(description = "Task ID", required = true) @PathVariable Long id,
            @RequestBody TaskRequest request) {
        Task task = toEntity(request);
        return ResponseEntity.ok(taskService.update(id, task));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change status", description = "Updates task status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    public ResponseEntity<Task> patchStatus(
            @Parameter(description = "Task ID", required = true) @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.patchStatus(id, status));
    }

    @PatchMapping("/{id}/assignee")
    @Operation(summary = "Assign/reassign", description = "Updates task assignee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    public ResponseEntity<Task> patchAssignee(
            @Parameter(description = "Task ID", required = true) @PathVariable Long id,
            @RequestParam Long assigneeId) {
        return ResponseEntity.ok(taskService.patchAssignee(id, assigneeId));
    }

    @PostMapping("/reorder")
    @Operation(summary = "Bulk reorder", description = "Reorders tasks by the given IDs (order = index).")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Void> reorder(@RequestBody List<Long> taskIds) {
        taskService.reorder(taskIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Deletes a task and its subtasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content)
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Task ID", required = true) @PathVariable Long id) {
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(
                    Sort.Order.asc("projectId"),
                    Sort.Order.asc("orderIndex"),
                    Sort.Order.desc("createdAt")
            );
        }
        String[] parts = sort.split(",");
        if (parts.length == 1) {
            return Sort.by(parts[0].trim());
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0].trim());
    }

    private static LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), java.time.ZoneId.systemDefault());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static Task toEntity(TaskRequest r) {
        return Task.builder()
                .projectId(r.getProjectId())
                .contractId(r.getContractId())
                .title(r.getTitle())
                .description(r.getDescription())
                .status(r.getStatus())
                .priority(r.getPriority())
                .assigneeId(r.getAssigneeId())
                .dueDate(r.getDueDate())
                .orderIndex(r.getOrderIndex())
                .parentTaskId(r.getParentTaskId())
                .createdBy(r.getCreatedBy())
                .build();
    }
}
