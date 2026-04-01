package com.example.taskmanagement.dto.auth;

import com.example.taskmanagement.dto.user.UserResponse;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
