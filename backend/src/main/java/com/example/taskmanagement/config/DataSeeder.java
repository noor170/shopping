package com.example.taskmanagement.config;

import com.example.taskmanagement.entity.AuditAction;
import com.example.taskmanagement.entity.AuditLog;
import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskComment;
import com.example.taskmanagement.entity.TaskStatus;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.entity.UserStatus;
import com.example.taskmanagement.repository.AuditLogRepository;
import com.example.taskmanagement.repository.TaskCommentRepository;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final AuditLogRepository auditLogRepository;

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository) {
        return args -> seedInitialData(userRepository);
    }

    @Transactional
    void seedInitialData(UserRepository userRepository) {
        if (userRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now();

        User admin = buildUser("System Admin", "admin", "admin@example.com", "Admin@123", Role.ADMIN, UserStatus.ACTIVE, now);
        User user = buildUser("Demo User", "user", "user@example.com", "User@123", Role.USER, UserStatus.ACTIVE, now);
        User userTwo = buildUser("Project Member", "member", "member@example.com", "Member@123", Role.USER, UserStatus.ACTIVE, now);
        User inactiveUser = buildUser("Inactive User", "inactive", "inactive@example.com", "Inactive@123", Role.USER, UserStatus.INACTIVE, now);

        userRepository.saveAll(List.of(admin, user, userTwo, inactiveUser));

        Task pendingTask = buildTask(
                "Prepare release checklist",
                "Draft the pending checklist for the next release.",
                TaskStatus.PENDING,
                user,
                null,
                null,
                null,
                now.minusSeconds(10_000));

        Task inProgressTask = buildTask(
                "Implement notification service",
                "Work in progress task assigned to demo user.",
                TaskStatus.IN_PROGRESS,
                user,
                null,
                null,
                null,
                now.minusSeconds(8_000));

        Task completedTask = buildTask(
                "Write API regression report",
                "Completed task ready for submission and admin review.",
                TaskStatus.COMPLETED,
                userTwo,
                null,
                now.minusSeconds(2_000),
                null,
                now.minusSeconds(6_000));

        Task approvedTask = buildTask(
                "Refresh onboarding guide",
                "Task already approved by admin for demo purposes.",
                TaskStatus.APPROVED,
                user,
                admin,
                now.minusSeconds(4_000),
                "Approved for publication",
                now.minusSeconds(5_000));

        Task rejectedTask = buildTask(
                "Update deployment notes",
                "Previously rejected task for retesting workflow.",
                TaskStatus.REJECTED,
                userTwo,
                admin,
                now.minusSeconds(3_500),
                "Needs more detail in rollback section",
                now.minusSeconds(4_500));

        taskRepository.saveAll(List.of(pendingTask, inProgressTask, completedTask, approvedTask, rejectedTask));

        taskCommentRepository.saveAll(List.of(
                buildComment("Initial draft has started.", pendingTask, user, now.minusSeconds(9_500)),
                buildComment("Admin reviewed the previous version.", approvedTask, admin, now.minusSeconds(4_200)),
                buildComment("Please improve the deployment rollback notes.", rejectedTask, admin, now.minusSeconds(3_900)),
                buildComment("Working on the notification flow integration.", inProgressTask, user, now.minusSeconds(7_200))
        ));

        auditLogRepository.saveAll(List.of(
                buildAudit("system", AuditAction.USER_REGISTERED, "USER", "1", "Seeded admin account", now.minusSeconds(20_000)),
                buildAudit("system", AuditAction.USER_REGISTERED, "USER", "2", "Seeded demo user account", now.minusSeconds(19_500)),
                buildAudit("admin", AuditAction.TASK_CREATED, "TASK", "1", "Seeded pending task", now.minusSeconds(10_000)),
                buildAudit("admin", AuditAction.TASK_ASSIGNED, "TASK", "3", "Assigned completed task to member", now.minusSeconds(6_000)),
                buildAudit("admin", AuditAction.TASK_APPROVED, "TASK", "4", "Approved onboarding guide task", now.minusSeconds(4_000)),
                buildAudit("admin", AuditAction.TASK_REJECTED, "TASK", "5", "Rejected deployment notes task", now.minusSeconds(3_800)),
                buildAudit("user", AuditAction.TASK_COMMENTED, "TASK", "2", "Added progress comment", now.minusSeconds(7_200))
        ));
    }

    private User buildUser(
            String fullName,
            String username,
            String email,
            String rawPassword,
            Role role,
            UserStatus status,
            Instant timestamp
    ) {
        User user = User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(status)
                .build();
        user.setCreatedAt(timestamp);
        user.setUpdatedAt(timestamp);
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        return user;
    }

    private Task buildTask(
            String title,
            String description,
            TaskStatus status,
            User owner,
            User reviewedBy,
            Instant submittedAt,
            String reviewComment,
            Instant timestamp
    ) {
        Task task = Task.builder()
                .title(title)
                .description(description)
                .status(status)
                .deleted(false)
                .submittedAt(submittedAt)
                .reviewedAt(reviewedBy != null ? timestamp.plusSeconds(600) : null)
                .reviewComment(reviewComment)
                .owner(owner)
                .reviewedBy(reviewedBy)
                .build();
        task.setCreatedAt(timestamp);
        task.setUpdatedAt(timestamp.plusSeconds(300));
        task.setCreatedBy(owner.getUsername());
        task.setUpdatedBy(reviewedBy != null ? reviewedBy.getUsername() : owner.getUsername());
        return task;
    }

    private TaskComment buildComment(String content, Task task, User author, Instant timestamp) {
        TaskComment comment = TaskComment.builder()
                .content(content)
                .task(task)
                .author(author)
                .build();
        comment.setCreatedAt(timestamp);
        comment.setUpdatedAt(timestamp);
        comment.setCreatedBy(author.getUsername());
        comment.setUpdatedBy(author.getUsername());
        return comment;
    }

    private AuditLog buildAudit(
            String actor,
            AuditAction action,
            String targetType,
            String targetId,
            String details,
            Instant timestamp
    ) {
        return AuditLog.builder()
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .createdAt(timestamp)
                .build();
    }
}
