package com.example.taskmanagement.dto.user;

import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String username,
        String email,
        Role role,
        UserStatus status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
