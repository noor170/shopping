package com.example.taskmanagement.dto.task;

import com.example.taskmanagement.entity.TaskImportance;
import com.example.taskmanagement.entity.TaskPriority;
import com.example.taskmanagement.entity.TaskUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull TaskPriority priority,
        @NotNull TaskImportance importance,
        @NotNull TaskUrgency urgency,
        LocalDate deadline,
        @Positive Long assigneeUserId
) {
}
