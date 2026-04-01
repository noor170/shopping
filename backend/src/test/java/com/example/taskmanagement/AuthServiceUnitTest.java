package com.example.taskmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanagement.dto.auth.AuthResponse;
import com.example.taskmanagement.dto.auth.RegisterRequest;
import com.example.taskmanagement.dto.user.UserResponse;
import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.entity.UserStatus;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.CustomUserDetails;
import com.example.taskmanagement.security.JwtService;
import com.example.taskmanagement.service.AuditService;
import com.example.taskmanagement.service.AuthService;
import com.example.taskmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("Name", "dupuser", "dup@example.com", "Password@123");
        when(userRepository.existsByUsernameIgnoreCase("dupuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void registerShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("Name", "unituser", "unit@example.com", "Password@123");
        User savedUser = User.builder()
                .fullName("Name")
                .username("unituser")
                .email("unit@example.com")
                .password("encoded")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        savedUser.setId(10L);
        savedUser.setCreatedBy("system");
        savedUser.setUpdatedBy("system");

        when(passwordEncoder.encode("Password@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("jwt-token");
        when(userService.toResponse(savedUser)).thenReturn(new UserResponse(
                10L, "Name", "unituser", "unit@example.com", Role.USER, UserStatus.ACTIVE,
                "system", "system", null, null));

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals("unituser", response.user().username());
        verify(auditService).log(any(), any(), any(), any());
    }
}
