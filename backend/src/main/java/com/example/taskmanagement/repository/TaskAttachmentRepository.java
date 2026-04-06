package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.TaskAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    Optional<TaskAttachment> findByIdAndTaskId(Long id, Long taskId);
}
