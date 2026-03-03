package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
import com.esprit.task.service.TaskNotificationService;
import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskCommentRepository;
import com.esprit.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.TaskCalendarEventDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private TaskNotificationService taskNotificationService;

    @InjectMocks
    private TaskService taskService;

    private static Task task(Long id) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1L);
        t.setTitle("Task " + id);
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        t.setDueDate(LocalDate.now().plusDays(7));
        return t;
    }

    @Test
    void findById_whenFound_returnsTask() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        Task result = taskService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Task 1");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void findByProjectId_returnsList() {
        Task t = task(1L);
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByProjectId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void getBoardByProject_returnsBoard() {
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(task(1L)));

        var result = taskService.getBoardByProject(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getColumns()).isNotNull();
    }

    @Test
    void getOverdueTasks_returnsList() {
        when(taskRepository.findOverdueTasks(LocalDate.now())).thenReturn(List.of(task(1L)));

        List<Task> result = taskService.getOverdueTasks(Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
    }

    @Test
    void getStatsByProject_returnsStats() {
        when(taskRepository.countByProjectId(1L)).thenReturn(10L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.DONE)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_REVIEW)).thenReturn(1L);
        when(taskRepository.findOverdueTasksByProject(1L, LocalDate.now())).thenReturn(List.of());

        TaskStatsDto result = taskService.getStatsByProject(1L);

        assertThat(result.getTotalTasks()).isEqualTo(10);
        assertThat(result.getDoneCount()).isEqualTo(5);
        assertThat(result.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    void create_setsDefaultsAndSaves() {
        Task t = task(null);
        t.setStatus(null);
        t.setPriority(null);
        t.setOrderIndex(null);
        when(taskRepository.findMaxOrderIndexByProject(1L)).thenReturn(0);
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Task result = taskService.create(t);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    void update_modifiesAndSaves() {
        Task existing = task(1L);
        Task updated = task(1L);
        updated.setTitle("Updated");
        updated.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenReturn(updated);

        Task result = taskService.update(1L, updated);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
        verify(taskNotificationService, never()).notifyTaskStatusUpdate(any());
    }

    @Test
    void update_whenStatusChanged_notifiesClient() {
        Task existing = task(1L);
        existing.setStatus(TaskStatus.TODO);
        Task updated = task(1L);
        updated.setTitle("Updated");
        updated.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task s = inv.getArgument(0);
            s.setStatus(TaskStatus.IN_PROGRESS);
            return s;
        });

        taskService.update(1L, updated);

        verify(taskNotificationService).notifyTaskStatusUpdate(any());
    }

    @Test
    void patchStatus_updatesStatusAndNotifies() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setStatus(TaskStatus.IN_PROGRESS);
            return saved;
        });

        taskService.patchStatus(1L, TaskStatus.IN_PROGRESS);

        verify(taskRepository).save(any());
        verify(taskNotificationService).notifyTaskStatusUpdate(any());
    }

    @Test
    void findAllFiltered_returnsPaginatedResults() {
        Task t = task(1L);
        Page<Task> page = new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Task> result = taskService.findAllFiltered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findByContractId_returnsList() {
        Task t = task(1L);
        when(taskRepository.findByContractIdOrderByProjectIdAscOrderIndexAsc(1L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByContractId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByAssigneeId_returnsList() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        when(taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(10L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByAssigneeId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(10L);
    }

    @Test
    void getStatsByFreelancer_returnsStats() {
        Task t = task(1L);
        t.setStatus(TaskStatus.DONE);
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(t, task(2L)));

        TaskStatsDto result = taskService.getStatsByFreelancer(10L, Optional.empty(), Optional.empty());

        assertThat(result.getTotalTasks()).isEqualTo(2);
        assertThat(result.getDoneCount()).isEqualTo(1);
    }

    @Test
    void getDashboardStats_returnsStats() {
        when(taskRepository.findAll()).thenReturn(List.of(task(1L), task(2L)));
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of());

        TaskStatsDto result = taskService.getDashboardStats();

        assertThat(result.getTotalTasks()).isEqualTo(2);
    }

    @Test
    void patchAssignee_updatesAssignee() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenReturn(t);

        taskService.patchAssignee(1L, 10L);

        verify(taskRepository).save(any());
    }

    @Test
    void reorder_updatesOrderIndex() {
        Task t = task(1L);
        t.setOrderIndex(5);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenReturn(t);

        taskService.reorder(List.of(1L));

        verify(taskRepository).save(any());
    }

    @Test
    void create_whenMaxOrderIndexNull_setsOrderIndexZero() {
        Task t = task(null);
        t.setStatus(null);
        t.setPriority(null);
        t.setOrderIndex(null);
        when(taskRepository.findMaxOrderIndexByProject(1L)).thenReturn(null);
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setOrderIndex(0);
            return saved;
        });

        Task result = taskService.create(t);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    void deleteById_deletesCommentsAndTask() {
        Task t = task(1L);
        com.esprit.task.entity.TaskComment c = new com.esprit.task.entity.TaskComment();
        c.setId(1L);
        c.setTaskId(1L);
        c.setUserId(10L);
        c.setMessage("x");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.findByParentTaskId(1L)).thenReturn(List.of());
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c));

        taskService.deleteById(1L);

        verify(taskCommentRepository).delete(c);
        verify(taskRepository).delete(t);
    }

    @Test
    void getCalendarEvents_withoutUserId_returnsEventsFromRepo() {
        Task t = task(1L);
        t.setDueDate(LocalDate.now().plusDays(1));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        when(taskRepository.findByDueDateBetween(min.toLocalDate(), max.toLocalDate()))
                .thenReturn(List.of(t));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("task-1");
        assertThat(result.get(0).getSummary()).contains("Task 1");
    }

    @Test
    void getCalendarEvents_withUserId_returnsMergedAssigneeAndProjectTasks() {
        Task t1 = task(1L);
        t1.setAssigneeId(5L);
        t1.setDueDate(LocalDate.now().plusDays(1));
        Task t2 = task(2L);
        t2.setProjectId(10L);
        t2.setDueDate(LocalDate.now().plusDays(2));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        LocalDate start = min.toLocalDate();
        LocalDate end = max.toLocalDate();

        when(projectClient.getProjectsByClientId(5L)).thenReturn(
                List.of(new ProjectDto(10L, 5L, "P", null)));
        when(taskRepository.findByDueDateBetweenAndAssigneeId(start, end, 5L)).thenReturn(List.of(t1));
        when(taskRepository.findByDueDateBetween(start, end)).thenReturn(List.of(t1, t2));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.of(5L));

        assertThat(result).hasSize(2);
    }

    @Test
    void getCalendarEvents_withUserId_whenProjectClientThrows_continuesWithoutProjectIds() {
        Task t = task(1L);
        t.setAssigneeId(5L);
        t.setDueDate(LocalDate.now().plusDays(1));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        LocalDate start = min.toLocalDate();
        LocalDate end = max.toLocalDate();

        when(projectClient.getProjectsByClientId(5L)).thenThrow(new RuntimeException("Network error"));
        when(taskRepository.findByDueDateBetweenAndAssigneeId(start, end, 5L)).thenReturn(List.of(t));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.of(5L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withProjectId_only_returnsFiltered() {
        Task t = task(1L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByProject(1L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.of(1L), Optional.empty());

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withAssigneeId_only_returnsFiltered() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByAssignee(10L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.empty(), Optional.of(10L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withBothProjectAndAssignee_filtersByBoth() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByProject(1L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.of(1L), Optional.of(10L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getStatsByProject_whenTotalZero_returnsZeroCompletion() {
        when(taskRepository.countByProjectId(1L)).thenReturn(0L);
        when(taskRepository.findOverdueTasksByProject(1L, LocalDate.now())).thenReturn(List.of());

        TaskStatsDto result = taskService.getStatsByProject(1L);

        assertThat(result.getTotalTasks()).isEqualTo(0);
        assertThat(result.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    void deleteById_withSubtasks_deletesRecursively() {
        Task parent = task(1L);
        Task sub = task(2L);
        sub.setParentTaskId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskId(1L)).thenReturn(List.of(sub));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(sub));
        when(taskRepository.findByParentTaskId(2L)).thenReturn(List.of());
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());

        taskService.deleteById(1L);

        verify(taskRepository).delete(sub);
        verify(taskRepository).delete(parent);
    }
}
