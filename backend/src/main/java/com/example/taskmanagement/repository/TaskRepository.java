package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByDeletedFalseAndOwnerIdAndStatusInAndTitleContainingIgnoreCase(
            Long ownerId, java.util.Collection<TaskStatus> statuses, String title, Pageable pageable);

    Page<Task> findByDeletedFalseAndStatusInAndTitleContainingIgnoreCase(
            java.util.Collection<TaskStatus> statuses, String title, Pageable pageable);
}
