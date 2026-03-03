package com.esprit.task.controller;

import com.esprit.task.entity.TaskComment;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.service.TaskCommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskCommentController.class)
class TaskCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskCommentService taskCommentService;

    private static TaskComment comment(Long id, Long taskId) {
        TaskComment c = new TaskComment();
        c.setId(id);
        c.setTaskId(taskId);
        c.setUserId(10L);
        c.setMessage("Test comment");
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void list_returnsPaginated() throws Exception {
        TaskComment c = comment(1L, 1L);
        Page<TaskComment> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
        when(taskCommentService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/task-comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Test comment"));
    }

    @Test
    void getById_returnsComment() throws Exception {
        TaskComment c = comment(1L, 1L);
        when(taskCommentService.findById(1L)).thenReturn(c);

        mockMvc.perform(get("/api/task-comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByTaskId_returnsList() throws Exception {
        TaskComment c = comment(1L, 1L);
        when(taskCommentService.findByTaskId(1L)).thenReturn(List.of(c));

        mockMvc.perform(get("/api/task-comments/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(1));
    }

    @Test
    void create_returns201() throws Exception {
        TaskComment c = comment(1L, 1L);
        when(taskCommentService.create(any())).thenReturn(c);

        String body = """
                {"taskId":1,"userId":10,"message":"New comment"}
                """;
        mockMvc.perform(post("/api/task-comments").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/task-comments/1"))
                .andExpect(status().isNoContent());
        verify(taskCommentService).deleteById(1L);
    }

    @Test
    void update_returns200() throws Exception {
        TaskComment c = comment(1L, 1L);
        c.setMessage("Updated");
        when(taskCommentService.update(eq(1L), any())).thenReturn(c);

        String body = """
                {"taskId":1,"userId":10,"message":"Updated message"}
                """;
        mockMvc.perform(put("/api/task-comments/1").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskCommentService.findById(999L)).thenThrow(new EntityNotFoundException("TaskComment", 999L));

        mockMvc.perform(get("/api/task-comments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
