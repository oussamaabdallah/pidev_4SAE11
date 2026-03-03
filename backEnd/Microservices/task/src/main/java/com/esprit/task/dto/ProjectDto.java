package com.esprit.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private Long clientId;
    private String title;
    private LocalDateTime deadline;
}
