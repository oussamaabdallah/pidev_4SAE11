package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
import com.esprit.task.service.TaskNotificationService;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.TaskBoardDto;
import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskCommentRepository;
import com.esprit.task.repository.TaskRepository;
import com.esprit.task.repository.TaskSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ProjectClient projectClient;
    private final TaskNotificationService taskNotificationService;

    @Transactional(readOnly = true)
    public Page<Task> findAllFiltered(Optional<Long> projectId, Optional<Long> contractId, Optional<Long> assigneeId,
                                      Optional<TaskStatus> status, Optional<TaskPriority> priority, Optional<Long> parentId,
                                      Optional<String> search, Optional<LocalDate> dueDateFrom, Optional<LocalDate> dueDateTo,
                                      Pageable pageable) {
        var spec = TaskSpecification.filtered(projectId, contractId, assigneeId, status, priority, parentId, search, dueDateFrom, dueDateTo);
        return taskRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task", id));
    }

    @Transactional(readOnly = true)
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<Task> findByContractId(Long contractId) {
        return taskRepository.findByContractIdOrderByProjectIdAscOrderIndexAsc(contractId);
    }

    @Transactional(readOnly = true)
    public List<Task> findByAssigneeId(Long assigneeId) {
        return taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(assigneeId);
    }

    @Transactional(readOnly = true)
    public TaskBoardDto getBoardByProject(Long projectId) {
        List<Task> all = taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
        Map<TaskStatus, List<Task>> columns = Arrays.stream(TaskStatus.values())
                .filter(s -> s != TaskStatus.CANCELLED)
                .collect(Collectors.toMap(s -> s, s -> new ArrayList<>()));
        for (Task t : all) {
            if (t.getStatus() != TaskStatus.CANCELLED && columns.containsKey(t.getStatus())) {
                columns.get(t.getStatus()).add(t);
            }
        }
        return TaskBoardDto.builder()
                .projectId(projectId)
                .columns(columns)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks(Optional<Long> projectId, Optional<Long> assigneeId) {
        LocalDate today = LocalDate.now();
        if (projectId.isPresent() && assigneeId.isPresent()) {
            return taskRepository.findOverdueTasksByProject(projectId.get(), today).stream()
                    .filter(t -> t.getAssigneeId() != null && t.getAssigneeId().equals(assigneeId.get()))
                    .toList();
        }
        if (projectId.isPresent()) {
            return taskRepository.findOverdueTasksByProject(projectId.get(), today);
        }
        if (assigneeId.isPresent()) {
            return taskRepository.findOverdueTasksByAssignee(assigneeId.get(), today);
        }
        return taskRepository.findOverdueTasks(today);
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getStatsByProject(Long projectId) {
        long total = taskRepository.countByProjectId(projectId);
        long done = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS)
                + taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_REVIEW);
        long overdue = taskRepository.findOverdueTasksByProject(projectId, LocalDate.now()).size();
        double completionPercentage = total > 0 ? (100.0 * done / total) : 0.0;
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(done)
                .inProgressCount(inProgress)
                .overdueCount(overdue)
                .completionPercentage(completionPercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getStatsByFreelancer(Long freelancerId, Optional<LocalDate> from, Optional<LocalDate> to) {
        var spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.of(freelancerId),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                from, to);
        List<Task> tasks = taskRepository.findAll(spec);
        long total = tasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW).count();
        long overdue = tasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED).count();
        double completionPercentage = total > 0 ? (100.0 * done / total) : 0.0;
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(done)
                .inProgressCount(inProgress)
                .overdueCount(overdue)
                .completionPercentage(completionPercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getDashboardStats() {
        List<Task> all = taskRepository.findAll();
        long total = all.size();
        long doneCount = all.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long inProgressCount = all.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW).count();
        long overdueCount = taskRepository.findOverdueTasks(LocalDate.now()).size();
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(doneCount)
                .inProgressCount(inProgressCount)
                .overdueCount(overdueCount)
                .completionPercentage(total > 0 ? (100.0 * doneCount / total) : 0.0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskCalendarEventDto> getCalendarEvents(LocalDateTime timeMin, LocalDateTime timeMax, Optional<Long> userId) {
        LocalDate start = timeMin.toLocalDate();
        LocalDate end = timeMax.toLocalDate();
        List<Task> tasks;
        if (userId.isPresent()) {
            Set<Long> projectIds = new HashSet<>();
            try {
                List<ProjectDto> clientProjects = projectClient.getProjectsByClientId(userId.get());
                if (clientProjects != null) {
                    clientProjects.stream().map(ProjectDto::getId).filter(Objects::nonNull).forEach(projectIds::add);
                }
            } catch (Exception e) {
                log.warn("Failed to load client projects for calendar filter: {}", e.getMessage());
            }
            List<Task> byAssignee = taskRepository.findByDueDateBetweenAndAssigneeId(start, end, userId.get());
            Set<Long> assigneeTaskIds = byAssignee.stream().map(Task::getId).collect(Collectors.toSet());
            List<Task> byProject = taskRepository.findByDueDateBetween(start, end).stream()
                    .filter(t -> projectIds.contains(t.getProjectId()) && !assigneeTaskIds.contains(t.getId()))
                    .toList();
            tasks = new ArrayList<>(byAssignee);
            tasks.addAll(byProject);
        } else {
            tasks = taskRepository.findByDueDateBetween(start, end);
        }
        return tasks.stream()
                .map(t -> TaskCalendarEventDto.builder()
                        .id("task-" + t.getId())
                        .summary("Task deadline – " + (t.getTitle() != null ? t.getTitle() : "Task #" + t.getId()))
                        .start(t.getDueDate().atStartOfDay())
                        .end(t.getDueDate().atStartOfDay().plus(1, ChronoUnit.HOURS))
                        .description(t.getDescription())
                        .build())
                .toList();
    }

    @Transactional
    public Task create(Task task) {
        if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
        if (task.getPriority() == null) task.setPriority(TaskPriority.MEDIUM);
        if (task.getOrderIndex() == null) {
            Integer max = taskRepository.findMaxOrderIndexByProject(task.getProjectId());
            task.setOrderIndex(max != null ? max + 1 : 0);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, Task task) {
        Task existing = findById(id);
        TaskStatus oldStatus = existing.getStatus();
        existing.setProjectId(task.getProjectId());
        existing.setContractId(task.getContractId());
        existing.setTitle(task.getTitle());
        existing.setDescription(task.getDescription());
        existing.setStatus(task.getStatus() != null ? task.getStatus() : existing.getStatus());
        existing.setPriority(task.getPriority() != null ? task.getPriority() : existing.getPriority());
        existing.setAssigneeId(task.getAssigneeId());
        existing.setDueDate(task.getDueDate());
        if (task.getOrderIndex() != null) existing.setOrderIndex(task.getOrderIndex());
        existing.setParentTaskId(task.getParentTaskId());
        Task saved = taskRepository.save(existing);
        if (task.getStatus() != null && !task.getStatus().equals(oldStatus)) {
            taskNotificationService.notifyTaskStatusUpdate(saved);
        }
        return saved;
    }

    @Transactional
    public Task patchStatus(Long id, TaskStatus status) {
        Task task = findById(id);
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        taskNotificationService.notifyTaskStatusUpdate(saved);
        return saved;
    }

    @Transactional
    public Task patchAssignee(Long id, Long assigneeId) {
        Task task = findById(id);
        task.setAssigneeId(assigneeId);
        return taskRepository.save(task);
    }

    @Transactional
    public void reorder(List<Long> taskIds) {
        for (int i = 0; i < taskIds.size(); i++) {
            final int orderIndex = i;
            taskRepository.findById(taskIds.get(i)).ifPresent(t -> {
                t.setOrderIndex(orderIndex);
                taskRepository.save(t);
            });
        }
    }

    @Transactional
    public void deleteById(Long id) {
        Task task = findById(id);
        taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(id).forEach(taskCommentRepository::delete);
        List<Task> subtasks = taskRepository.findByParentTaskId(id);
        for (Task st : subtasks) {
            deleteById(st.getId());
        }
        taskRepository.delete(task);
    }
}
