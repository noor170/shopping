package com.example.taskmanagement.dto.task;

import jakarta.validation.constraints.Size;

public record TaskReviewRequest(
        @Size(max = 500) String comment
) {
}
