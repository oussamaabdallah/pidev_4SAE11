package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenReturn(updated);

        Task result = taskService.update(1L, updated);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    void patchStatus_updatesStatus() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenReturn(t);

        taskService.patchStatus(1L, TaskStatus.IN_PROGRESS);

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
}
