package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.auth.AuthRequest;
import com.example.taskmanagement.dto.auth.AuthResponse;
import com.example.taskmanagement.dto.auth.RegisterRequest;
import com.example.taskmanagement.entity.AuditAction;
import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.entity.UserStatus;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.CustomUserDetails;
import com.example.taskmanagement.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);
        auditService.log(AuditAction.USER_REGISTERED, "USER", savedUser.getId().toString(), "New user registered");
        return new AuthResponse(token, userService.toResponse(savedUser));
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userService.getByUsername(userDetails.getUsername());
        String token = jwtService.generateToken(userDetails);
        auditService.log(AuditAction.LOGIN_SUCCESS, "USER", user.getId().toString(), "Login successful");
        return new AuthResponse(token, userService.toResponse(user));
    }
}
