package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.task.TaskAssignRequest;
import com.example.taskmanagement.dto.task.TaskAttachmentResponse;
import com.example.taskmanagement.dto.task.TaskCommentCreateRequest;
import com.example.taskmanagement.dto.task.TaskCommentResponse;
import com.example.taskmanagement.dto.task.TaskCreateRequest;
import com.example.taskmanagement.dto.task.TaskResponse;
import com.example.taskmanagement.dto.task.TaskReviewRequest;
import com.example.taskmanagement.dto.task.TaskUpdateRequest;
import com.example.taskmanagement.entity.AuditAction;
import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskAttachment;
import com.example.taskmanagement.entity.TaskComment;
import com.example.taskmanagement.entity.TaskPriority;
import com.example.taskmanagement.entity.TaskStatus;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.TaskCommentRepository;
import com.example.taskmanagement.repository.TaskAttachmentRepository;
import com.example.taskmanagement.repository.TaskRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final TaskExportService taskExportService;
    private final UserService userService;
    private final AuditService auditService;

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse createTask(Authentication authentication, TaskCreateRequest request) {
        boolean isAdmin = hasRole(authentication, Role.ADMIN);
        User owner = isAdmin && request.assigneeUserId() != null
                ? userService.getAssignableUser(request.assigneeUserId())
                : userService.getByUsername(authentication.getName());
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(TaskStatus.PENDING)
                .priority(request.priority())
                .deadline(request.deadline())
                .deleted(false)
                .owner(owner)
                .build();
        Task saved = taskRepository.save(task);
        auditService.log(AuditAction.TASK_CREATED, "TASK", saved.getId().toString(), "Task created");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TaskResponse assignTask(Long taskId, TaskAssignRequest request) {
        Task task = getTask(taskId);
        if (task.isDeleted()) {
            throw new BadRequestException("Deleted tasks cannot be assigned");
        }

        User assignee = userService.getAssignableUser(request.assigneeUserId());
        task.setOwner(assignee);

        if (task.getStatus() == TaskStatus.APPROVED) {
            task.setStatus(TaskStatus.PENDING);
            task.setReviewedAt(null);
            task.setReviewedBy(null);
            task.setReviewComment(null);
        }

        auditService.log(AuditAction.TASK_ASSIGNED, "TASK", task.getId().toString(),
                "Task assigned to " + assignee.getUsername());
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskResponse updateTask(Authentication authentication, Long taskId, TaskUpdateRequest request) {
        Task task = getAccessibleTask(authentication, taskId);
        boolean isAdmin = hasRole(authentication, Role.ADMIN);
        if (!isAdmin && (task.getStatus() == TaskStatus.APPROVED || task.isDeleted())) {
            throw new BadRequestException("Task cannot be updated in its current state");
        }
        if (task.isDeleted()) {
            throw new BadRequestException("Deleted tasks cannot be updated");
        }
        if (!isAdmin && !isUserManagedStatus(request.status())) {
            throw new BadRequestException("Users can set status only to PENDING, IN_PROGRESS, or COMPLETED");
        }
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
        task.setStatus(request.status());
        task.setSubmittedAt(null);
        if (request.status() != TaskStatus.APPROVED) {
            task.setReviewedAt(null);
            task.setReviewedBy(null);
        }
        if (request.status() != TaskStatus.REJECTED) {
            task.setReviewComment(null);
        }
        auditService.log(AuditAction.TASK_UPDATED, "TASK", task.getId().toString(), "Task updated");
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public void softDeleteTask(Authentication authentication, Long taskId) {
        Task task = getAccessibleTask(authentication, taskId);
        boolean isAdmin = hasRole(authentication, Role.ADMIN);
        if (!isAdmin && task.getStatus() == TaskStatus.APPROVED) {
            throw new BadRequestException("Approved tasks cannot be deleted");
        }
        if (task.isDeleted()) {
            throw new BadRequestException("Task is already deleted");
        }
        task.setDeleted(true);
        auditService.log(AuditAction.TASK_DELETED, "TASK", task.getId().toString(), "Task soft deleted");
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TaskResponse submitTask(String username, Long taskId) {
        Task task = getOwnedTask(username, taskId);
        if (task.isDeleted()) {
            throw new BadRequestException("Deleted task cannot be submitted");
        }
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BadRequestException("Only completed tasks can be submitted for approval");
        }
        task.setSubmittedAt(Instant.now());
        task.setReviewComment(null);
        task.setReviewedAt(null);
        task.setReviewedBy(null);
        auditService.log(AuditAction.TASK_SUBMITTED, "TASK", task.getId().toString(), "Task submitted for approval");
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TaskResponse approveTask(String adminUsername, Long taskId, TaskReviewRequest request) {
        User admin = userService.getByUsername(adminUsername);
        Task task = getTask(taskId);
        ensureSubmitted(task);
        task.setStatus(TaskStatus.APPROVED);
        task.setReviewComment(request.comment());
        task.setReviewedAt(Instant.now());
        task.setReviewedBy(admin);
        auditService.log(AuditAction.TASK_APPROVED, "TASK", task.getId().toString(), "Task approved");
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TaskResponse rejectTask(String adminUsername, Long taskId, TaskReviewRequest request) {
        User admin = userService.getByUsername(adminUsername);
        Task task = getTask(taskId);
        ensureSubmitted(task);
        task.setStatus(TaskStatus.REJECTED);
        task.setReviewComment(request.comment());
        task.setReviewedAt(Instant.now());
        task.setReviewedBy(admin);
        auditService.log(AuditAction.TASK_REJECTED, "TASK", task.getId().toString(), "Task rejected");
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskCommentResponse addComment(Authentication authentication, Long taskId, TaskCommentCreateRequest request) {
        Task task = getAccessibleTask(authentication, taskId);
        if (task.isDeleted()) {
            throw new BadRequestException("Deleted tasks cannot be commented on");
        }

        User author = userService.getByUsername(authentication.getName());
        TaskComment comment = taskCommentRepository.save(TaskComment.builder()
                .content(request.content())
                .task(task)
                .author(author)
                .build());

        auditService.log(AuditAction.TASK_COMMENTED, "TASK", task.getId().toString(), "Task comment added");
        return toCommentResponse(comment);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskAttachmentResponse uploadAttachment(Authentication authentication, Long taskId, MultipartFile file) {
        Task task = getAccessibleTask(authentication, taskId);
        if (task.isDeleted()) {
            throw new BadRequestException("Deleted tasks cannot accept attachments");
        }

        AttachmentStorageService.StoredFile storedFile = attachmentStorageService.store(file);
        TaskAttachment attachment = taskAttachmentRepository.save(TaskAttachment.builder()
                .originalFilename(storedFile.originalFilename())
                .storedFilename(storedFile.storedFilename())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .task(task)
                .build());

        auditService.log(
                AuditAction.TASK_ATTACHMENT_UPLOADED,
                "TASK",
                task.getId().toString(),
                "Attachment uploaded: " + attachment.getOriginalFilename()
        );

        return toAttachmentResponse(attachment);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TaskAttachment getAttachment(Authentication authentication, Long taskId, Long attachmentId) {
        Task task = getAccessibleTask(authentication, taskId);
        if (task.isDeleted()) {
            throw new ResourceNotFoundException("Attachment not found");
        }
        return taskAttachmentRepository.findByIdAndTaskId(attachmentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(TaskAttachment attachment) {
        return attachmentStorageService.loadAsResource(attachment.getStoredFilename());
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(
            String username,
            String role,
            TaskStatus status,
            TaskPriority priority,
            Long ownerUserId,
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = search == null ? "" : search.trim();
        List<TaskStatus> statuses = status == null ? Arrays.stream(TaskStatus.values()).toList() : List.of(status);
        List<TaskPriority> priorities = priority == null ? Arrays.stream(TaskPriority.values()).toList() : List.of(priority);

        if (Role.ADMIN.name().equals(role)) {
            if (ownerUserId != null) {
                return taskRepository.findByDeletedFalseAndOwnerIdAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                                ownerUserId, statuses, priorities, normalizedSearch, pageable)
                        .map(this::toResponse);
            }
            return taskRepository.findByDeletedFalseAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                            statuses, priorities, normalizedSearch, pageable)
                    .map(this::toResponse);
        }

        User owner = userService.getByUsername(username);
        return taskRepository.findByDeletedFalseAndOwnerIdAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                        owner.getId(), statuses, priorities, normalizedSearch, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public byte[] exportTasksPdf(
            String username,
            String role,
            TaskStatus status,
            TaskPriority priority,
            Long ownerUserId,
            String search
    ) {
        List<Task> tasks = findVisibleTasks(username, role, status, priority, ownerUserId, search);
        auditService.log(AuditAction.TASK_EXPORTED, "TASK", username, "Tasks exported as PDF");
        return taskExportService.exportPdf(tasks, username);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public byte[] exportTasksExcel(
            String username,
            String role,
            TaskStatus status,
            TaskPriority priority,
            Long ownerUserId,
            String search
    ) {
        List<Task> tasks = findVisibleTasks(username, role, status, priority, ownerUserId, search);
        auditService.log(AuditAction.TASK_EXPORTED, "TASK", username, "Tasks exported as Excel");
        return taskExportService.exportExcel(tasks);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(String username, String role, Long taskId) {
        Task task = getTask(taskId);
        if (Role.USER.name().equals(role) && !task.getOwner().getUsername().equalsIgnoreCase(username)) {
            throw new ResourceNotFoundException("Task not found");
        }
        if (task.isDeleted()) {
            throw new ResourceNotFoundException("Task not found");
        }
        return toResponse(task);
    }

    private Task getOwnedTask(String username, Long taskId) {
        Task task = getTask(taskId);
        if (!task.getOwner().getUsername().equalsIgnoreCase(username)) {
            throw new ResourceNotFoundException("Task not found");
        }
        return task;
    }

    private Task getAccessibleTask(Authentication authentication, Long taskId) {
        if (hasRole(authentication, Role.ADMIN)) {
            return getTask(taskId);
        }
        return getOwnedTask(authentication.getName(), taskId);
    }

    private boolean hasRole(Authentication authentication, Role role) {
        String authority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void ensureSubmitted(Task task) {
        if (task.isDeleted() || task.getStatus() != TaskStatus.COMPLETED || task.getSubmittedAt() == null) {
            throw new BadRequestException("Only submitted completed tasks can be reviewed");
        }
    }

    private boolean isUserManagedStatus(TaskStatus status) {
        return status == TaskStatus.PENDING || status == TaskStatus.IN_PROGRESS || status == TaskStatus.COMPLETED;
    }

    private List<Task> findVisibleTasks(
            String username,
            String role,
            TaskStatus status,
            TaskPriority priority,
            Long ownerUserId,
            String search
    ) {
        String normalizedSearch = search == null ? "" : search.trim();
        List<TaskStatus> statuses = status == null ? Arrays.stream(TaskStatus.values()).toList() : List.of(status);
        List<TaskPriority> priorities = priority == null ? Arrays.stream(TaskPriority.values()).toList() : List.of(priority);

        if (Role.ADMIN.name().equals(role)) {
            if (ownerUserId != null) {
                return taskRepository.findByDeletedFalseAndOwnerIdAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                        ownerUserId, statuses, priorities, normalizedSearch, org.springframework.data.domain.Pageable.unpaged()).getContent();
            }
            return taskRepository.findByDeletedFalseAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                    statuses, priorities, normalizedSearch, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        User owner = userService.getByUsername(username);
        return taskRepository.findByDeletedFalseAndOwnerIdAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
                owner.getId(), statuses, priorities, normalizedSearch, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.isDeleted(),
                task.getSubmittedAt(),
                task.getReviewedAt(),
                task.getReviewComment(),
                task.getOwner().getId(),
                task.getOwner().getUsername(),
                task.getReviewedBy() != null ? task.getReviewedBy().getId() : null,
                task.getReviewedBy() != null ? task.getReviewedBy().getUsername() : null,
                task.getCreatedBy(),
                task.getUpdatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                taskAttachmentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                        .map(this::toAttachmentResponse)
                        .toList(),
                taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                        .map(this::toCommentResponse)
                        .toList());
    }

    private TaskAttachmentResponse toAttachmentResponse(TaskAttachment attachment) {
        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                "/api/tasks/" + attachment.getTask().getId() + "/attachments/" + attachment.getId(),
                attachment.getCreatedBy(),
                attachment.getCreatedAt());
    }

    private TaskCommentResponse toCommentResponse(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getCreatedBy(),
                comment.getUpdatedBy(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
