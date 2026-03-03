package com.esprit.task.controller;

import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@TestPropertySource(properties = "welcome.message=Welcome to Task API")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
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
    void getFiltered_returnsPaginatedResults() throws Exception {
        Task t = task(1L);
        Page<Task> page = new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1);
        when(taskService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returnsTask() throws Exception {
        Task t = task(1L);
        when(taskService.findById(1L)).thenReturn(t);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Task 1"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskService.findById(999L)).thenThrow(new EntityNotFoundException("Task", 999L));

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getByProjectId_returnsList() throws Exception {
        Task t = task(1L);
        when(taskService.findByProjectId(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getBoardByProject_returnsBoard() throws Exception {
        when(taskService.getBoardByProject(1L)).thenReturn(
                com.esprit.task.dto.TaskBoardDto.builder().projectId(1L).columns(Map.of()).build());

        mockMvc.perform(get("/api/tasks/board/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void getOverdue_returnsList() throws Exception {
        when(taskService.getOverdueTasks(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCalendarEvents_returnsEvents() throws Exception {
        TaskCalendarEventDto evt = TaskCalendarEventDto.builder()
                .id("task-1")
                .summary("Task deadline – Test")
                .start(LocalDateTime.now())
                .end(LocalDateTime.now().plusHours(1))
                .build();
        when(taskService.getCalendarEvents(any(), any(), any())).thenReturn(List.of(evt));

        mockMvc.perform(get("/api/tasks/calendar-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-1"));
    }

    @Test
    void create_returns201() throws Exception {
        Task t = task(1L);
        when(taskService.create(any())).thenReturn(t);

        String body = """
                {"projectId":1,"title":"New Task","status":"TODO","priority":"MEDIUM"}
                """;
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_returns200() throws Exception {
        Task t = task(1L);
        when(taskService.update(eq(1L), any())).thenReturn(t);

        String body = """
                {"projectId":1,"title":"Updated","status":"IN_PROGRESS","priority":"HIGH"}
                """;
        mockMvc.perform(put("/api/tasks/1").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void patchStatus_returns200() throws Exception {
        Task t = task(1L);
        t.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.patchStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(t);

        mockMvc.perform(patch("/api/tasks/1/status").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
        verify(taskService).deleteById(1L);
    }
}
