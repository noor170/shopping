package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.task.TaskAssignRequest;
import com.example.taskmanagement.dto.task.TaskCommentCreateRequest;
import com.example.taskmanagement.dto.task.TaskCommentResponse;
import com.example.taskmanagement.dto.task.TaskCreateRequest;
import com.example.taskmanagement.dto.task.TaskResponse;
import com.example.taskmanagement.dto.task.TaskReviewRequest;
import com.example.taskmanagement.dto.task.TaskUpdateRequest;
import com.example.taskmanagement.entity.TaskPriority;
import com.example.taskmanagement.entity.TaskStatus;
import com.example.taskmanagement.security.CustomUserDetails;
import com.example.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public TaskResponse createTask(Authentication authentication, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(authentication, request);
    }

    @PostMapping("/{taskId}/assign")
    public TaskResponse assignTask(@PathVariable Long taskId, @Valid @RequestBody TaskAssignRequest request) {
        return taskService.assignTask(taskId, request);
    }

    @PostMapping("/{taskId}/comments")
    public TaskCommentResponse addComment(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCommentCreateRequest request
    ) {
        return taskService.addComment(authentication, taskId, request);
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request
    ) {
        return taskService.updateTask(authentication, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    public void deleteTask(Authentication authentication, @PathVariable Long taskId) {
        taskService.softDeleteTask(authentication, taskId);
    }

    @PostMapping("/{taskId}/submit")
    public TaskResponse submitTask(Authentication authentication, @PathVariable Long taskId) {
        return taskService.submitTask(authentication.getName(), taskId);
    }

    @PostMapping("/{taskId}/approve")
    public TaskResponse approveTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskReviewRequest request
    ) {
        return taskService.approveTask(authentication.getName(), taskId, request);
    }

    @PostMapping("/{taskId}/reject")
    public TaskResponse rejectTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskReviewRequest request
    ) {
        return taskService.rejectTask(authentication.getName(), taskId, request);
    }

    @GetMapping
    public Page<TaskResponse> getTasks(
            Authentication authentication,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "updatedAt") Pageable pageable
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return taskService.getTasks(authentication.getName(), role, status, priority, search, pageable);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTask(Authentication authentication, @PathVariable Long taskId) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return taskService.getTaskById(authentication.getName(), role, taskId);
    }
}
