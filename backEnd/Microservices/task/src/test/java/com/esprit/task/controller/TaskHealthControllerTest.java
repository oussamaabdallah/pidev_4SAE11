package com.esprit.task.controller;

import com.esprit.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskHealthController.class)
class TaskHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskRepository taskRepository;

    @Test
    void health_whenDbUp_returns200() throws Exception {
        when(taskRepository.count()).thenReturn(5L);

        mockMvc.perform(get("/api/task/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("task"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database.taskCount").value(5));
    }

    @Test
    void health_whenDbFails_returns503() throws Exception {
        when(taskRepository.count()).thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/task/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DEGRADED"));
    }
}
