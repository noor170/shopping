package com.example.taskmanagement.dto.user;

import com.example.taskmanagement.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull UserStatus status
) {
}
