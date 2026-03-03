package com.esprit.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calendar event (matches Planning CalendarEventDto for calendar integration)")
public class TaskCalendarEventDto {

    @Schema(description = "Event ID (e.g. task-1)")
    private String id;

    @Schema(description = "Event summary/title")
    private String summary;

    @Schema(description = "Start time")
    private LocalDateTime start;

    @Schema(description = "End time")
    private LocalDateTime end;

    @Schema(description = "Optional description")
    private String description;
}
