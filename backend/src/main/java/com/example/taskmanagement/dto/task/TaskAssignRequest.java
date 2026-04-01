package com.example.taskmanagement.dto.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaskAssignRequest(
        @NotNull @Positive Long assigneeUserId
) {
}
