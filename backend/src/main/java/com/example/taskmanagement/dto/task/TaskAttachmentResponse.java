package com.example.taskmanagement.dto.task;

import java.time.Instant;

public record TaskAttachmentResponse(
        Long id,
        String originalFilename,
        String contentType,
        long fileSize,
        String downloadPath,
        String createdBy,
        Instant createdAt
) {
}
