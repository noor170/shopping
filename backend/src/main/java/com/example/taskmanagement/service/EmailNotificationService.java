package com.example.taskmanagement.service;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.from:no-reply@taskmanagement.local}")
    private String fromAddress;

    public void sendTaskAssigned(User recipient, Task task) {
        send(
                recipient,
                "Task assigned: " + task.getTitle(),
                """
                Hello %s,

                A task has been assigned to you.

                Title: %s
                Status: %s
                Priority: %s
                Deadline: %s
                """.formatted(
                        recipient.getFullName(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDeadline() == null ? "-" : task.getDeadline())
        );
    }

    public void sendTaskCompleted(User recipient, Task task) {
        send(
                recipient,
                "Task completed: " + task.getTitle(),
                """
                Hello %s,

                Your task is now marked as completed.

                Title: %s
                Status: %s
                Priority: %s
                Deadline: %s
                """.formatted(
                        recipient.getFullName(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDeadline() == null ? "-" : task.getDeadline())
        );
    }

    public void sendTaskRejected(User recipient, Task task) {
        send(
                recipient,
                "Task rejected: " + task.getTitle(),
                """
                Hello %s,

                Your task has been rejected and needs updates.

                Title: %s
                Status: %s
                Review Comment: %s
                """.formatted(
                        recipient.getFullName(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getReviewComment() == null ? "-" : task.getReviewComment())
        );
    }

    private void send(User recipient, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would send to {} with subject '{}'", recipient.getEmail(), subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient.getEmail());
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send email to {} with subject '{}': {}", recipient.getEmail(), subject, ex.getMessage());
        }
    }
}
