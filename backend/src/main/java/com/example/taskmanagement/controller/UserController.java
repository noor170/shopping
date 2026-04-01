package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.user.UserResponse;
import com.example.taskmanagement.dto.user.UserStatusUpdateRequest;
import com.example.taskmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getMyProfile(authentication.getName());
    }

    @GetMapping
    public Page<UserResponse> getAllUsers(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @PatchMapping("/{userId}/status")
    public UserResponse updateStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusUpdateRequest request) {
        return userService.updateStatus(userId, request);
    }
}
