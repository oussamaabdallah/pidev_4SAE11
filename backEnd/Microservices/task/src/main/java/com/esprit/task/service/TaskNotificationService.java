package com.esprit.task.service;

import com.esprit.task.client.NotificationClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.NotificationRequestDto;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications to the Notification microservice when a task's status changes.
 * Failures are logged but do not affect the calling flow (fire-and-forget).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskNotificationService {

    private final NotificationClient notificationClient;
    private final ProjectClient projectClient;

    public static final String TYPE_TASK_STATUS_UPDATE = "TASK_STATUS_UPDATE";

    /**
     * Notify the project client when a task's status was updated.
     * Fetches the project to obtain clientId, then sends a notification.
     */
    public void notifyTaskStatusUpdate(Task task) {
        if (task == null || task.getProjectId() == null) {
            return;
        }
        ProjectDto project;
        try {
            project = projectClient.getProjectById(task.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to load project {} for task status notification: {}", task.getProjectId(), e.getMessage());
            return;
        }
        if (project == null || project.getClientId() == null) {
            return;
        }
        String userId = String.valueOf(project.getClientId());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "Task #" + task.getId();
        String statusLabel = task.getStatus() != null ? task.getStatus().name().replace("_", " ") : "updated";
        String title = "Task status updated";
        String body = String.format("Task \"%s\" is now %s.", taskTitle, statusLabel);

        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(task.getProjectId()));
        data.put("taskId", String.valueOf(task.getId()));

        notifyUser(userId, title, body, TYPE_TASK_STATUS_UPDATE, data);
    }

    private void notifyUser(String userId, String title, String body, String type, Map<String, String> data) {
        if (userId == null || title == null) {
            return;
        }
        try {
            NotificationRequestDto request = NotificationRequestDto.builder()
                    .userId(userId)
                    .title(title)
                    .body(body != null ? body : "")
                    .type(type != null ? type : "GENERAL")
                    .data(data)
                    .build();
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
}
