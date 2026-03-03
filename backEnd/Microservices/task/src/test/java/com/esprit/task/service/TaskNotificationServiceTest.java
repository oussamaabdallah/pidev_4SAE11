package com.esprit.task.service;

import com.esprit.task.client.NotificationClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskNotificationService. Verifies notifyTaskStatusUpdate
 * fetches project, builds correct notification payload, and calls NotificationClient.
 */
@ExtendWith(MockitoExtension.class)
class TaskNotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private ProjectClient projectClient;

    @InjectMocks
    private TaskNotificationService taskNotificationService;

    private static Task task(Long id) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1L);
        t.setTitle("Test Task");
        t.setStatus(TaskStatus.IN_PROGRESS);
        return t;
    }

    @Test
    void notifyTaskStatusUpdate_callsClientWithCorrectPayload() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient).create(captor.capture());
        var dto = captor.getValue();
        assertThat(dto.getUserId()).isEqualTo("100");
        assertThat(dto.getTitle()).isEqualTo("Task status updated");
        assertThat(dto.getBody()).contains("Test Task").contains("IN PROGRESS");
        assertThat(dto.getType()).isEqualTo(TaskNotificationService.TYPE_TASK_STATUS_UPDATE);
        assertThat(dto.getData()).containsEntry("projectId", "1").containsEntry("taskId", "1");
    }

    @Test
    void notifyTaskStatusUpdate_whenTaskNull_doesNotCallClient() {
        taskNotificationService.notifyTaskStatusUpdate(null);
        verify(notificationClient, never()).create(any());
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectIdNull_doesNotCallClient() {
        Task t = task(1L);
        t.setProjectId(null);
        taskNotificationService.notifyTaskStatusUpdate(t);
        verify(notificationClient, never()).create(any());
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectNotFound_doesNotCallNotification() {
        Task t = task(1L);
        when(projectClient.getProjectById(1L)).thenThrow(new RuntimeException("Not found"));

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectNull_doesNotCallNotification() {
        Task t = task(1L);
        when(projectClient.getProjectById(1L)).thenReturn(null);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectClientIdNull_doesNotCallNotification() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, null, "Project A", null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenTaskTitleNull_usesFallback() {
        Task t = task(1L);
        t.setTitle(null);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);
        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient).create(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Task #1");
    }

    @Test
    void notifyTaskStatusUpdate_whenNotificationClientThrows_doesNotPropagate() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null);
        when(projectClient.getProjectById(1L)).thenReturn(project);
        when(notificationClient.create(any())).thenThrow(new RuntimeException("Service unavailable"));

        taskNotificationService.notifyTaskStatusUpdate(t);

        // Should not throw - failures are logged and swallowed
    }
}
