package com.example.taskmanagement.dto.task;

import com.example.taskmanagement.entity.TaskStatus;
import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        boolean deleted,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewComment,
        Long ownerId,
        String ownerUsername,
        Long reviewedById,
        String reviewedByUsername,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        List<TaskCommentResponse> comments
) {
}
