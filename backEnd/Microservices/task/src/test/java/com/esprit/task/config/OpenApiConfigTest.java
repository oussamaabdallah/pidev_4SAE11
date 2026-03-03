package com.esprit.task.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void taskOpenAPI_returnsOpenAPIWithExpectedInfo() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "serverPort", "8091");

        OpenAPI api = config.taskOpenAPI();

        assertThat(api).isNotNull();
        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Tasks & Subtasks API");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(api.getInfo().getDescription()).contains("Tasks");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("http://localhost:8091");
    }
}
