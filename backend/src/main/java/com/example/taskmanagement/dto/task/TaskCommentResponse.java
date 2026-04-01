package com.example.taskmanagement.dto.task;

import java.time.Instant;

public record TaskCommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorUsername,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
