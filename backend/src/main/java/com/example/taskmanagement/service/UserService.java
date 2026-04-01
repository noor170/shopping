package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.user.UserResponse;
import com.example.taskmanagement.dto.user.UserStatusUpdateRequest;
import com.example.taskmanagement.entity.AuditAction;
import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.entity.UserStatus;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(this::toResponse)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(request.status());
        auditService.log(AuditAction.USER_STATUS_CHANGED, "USER", user.getId().toString(), "Status changed to " + request.status());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getAssignableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new BadRequestException("Tasks can only be assigned to USER accounts");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tasks can only be assigned to active users");
        }
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedBy(),
                user.getUpdatedBy(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
