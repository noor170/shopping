package com.example.taskmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.entity.UserStatus;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.service.AuditService;
import com.example.taskmanagement.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private UserService userService;

    @Test
    void getAssignableUserShouldReturnActiveNormalUser() {
        User user = User.builder().role(Role.USER).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        User result = userService.getAssignableUser(2L);

        assertEquals(Role.USER, result.getRole());
    }

    @Test
    void getAssignableUserShouldRejectInactiveUser() {
        User user = User.builder().role(Role.USER).status(UserStatus.INACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.getAssignableUser(2L));
    }

    @Test
    void getAssignableUserShouldThrowWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getAssignableUser(99L));
    }
}
