package com.example.taskmanagement.service;

import com.example.taskmanagement.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "pdf", "doc", "docx", "xls", "xlsx");

    private final Path storageDirectory;

    public AttachmentStorageService(@Value("${app.storage.task-attachments-dir:uploads/task-attachments}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize attachment storage", ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required");
        }

        String originalFilename = file.getOriginalFilename() == null ? "attachment" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Only png, pdf, doc, docx, xls, and xlsx files are allowed");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = storageDirectory.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store attachment file", ex);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = fallbackContentType(extension);
        }

        return new StoredFile(originalFilename, storedFilename, contentType, file.getSize());
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path filePath = storageDirectory.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Attachment file is not available");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read attachment file", ex);
        }
    }

    public record StoredFile(String originalFilename, String storedFilename, String contentType, long fileSize) {
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BadRequestException("File extension is required");
        }
        return filename.substring(index + 1).toLowerCase();
    }

    private String fallbackContentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}
