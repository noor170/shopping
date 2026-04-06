package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskPriority;
import com.example.taskmanagement.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByDeletedFalseAndOwnerIdAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
            Long ownerId,
            java.util.Collection<TaskStatus> statuses,
            java.util.Collection<TaskPriority> priorities,
            String title,
            Pageable pageable);

    Page<Task> findByDeletedFalseAndStatusInAndPriorityInAndTitleContainingIgnoreCase(
            java.util.Collection<TaskStatus> statuses,
            java.util.Collection<TaskPriority> priorities,
            String title,
            Pageable pageable);
}
