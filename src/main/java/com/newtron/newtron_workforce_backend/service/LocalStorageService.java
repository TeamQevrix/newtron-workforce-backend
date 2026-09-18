package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@Service
public class LocalStorageService implements StorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    @Value("${app.file-storage.base-path:./uploads}")
    private String basePath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(basePath, "profiles"));
            Files.createDirectories(Paths.get(basePath, "documents"));
            Files.createDirectories(Paths.get(basePath, "jobs"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    @Override
    public String uploadProfilePhoto(MultipartFile file) {
        validateFile(file, Arrays.asList("image/jpeg", "image/png", "image/webp"));
        return saveFile(file, "profiles");
    }

    @Override
    public String uploadDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FILE_EMPTY", "File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("FILE_TOO_LARGE", "File size exceeds the limit of 5 MB");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (contentType == null || contentType.equalsIgnoreCase("application/octet-stream")) {
            if (originalFilename != null) {
                String lowerName = originalFilename.toLowerCase();
                if (lowerName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (lowerName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowerName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                }
            }
        }

        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/webp", "application/pdf");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ValidationException("INVALID_FILE_TYPE", "Only JPEG, PNG, WEBP, and PDF files are allowed");
        }

        return saveFile(file, "documents");
    }

    private void validateFile(MultipartFile file, List<String> allowedMimeTypes) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FILE_EMPTY", "File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("FILE_TOO_LARGE", "File size exceeds the limit of 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new ValidationException("INVALID_FILE_TYPE", "Invalid file type. Allowed: " + String.join(", ", allowedMimeTypes));
        }
    }

    private String saveFile(MultipartFile file, String subDir) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Sanitize extension
        extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        
        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = Paths.get(basePath, subDir, filename).normalize();
        
        if (!targetPath.startsWith(Paths.get(basePath).normalize())) {
            throw new ValidationException("PATH_TRAVERSAL", "Invalid path");
        }
        
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ValidationException("FILE_WRITE_ERROR", "Failed to write file to disk");
        }
        
        return subDir + "/" + filename;
    }

    @Override
    public void deletePhoto(String key) {
        if (key != null && !key.contains("..")) {
            try {
                Path targetPath = Paths.get(basePath, key).normalize();
                if (targetPath.startsWith(Paths.get(basePath).normalize())) {
                    Files.deleteIfExists(targetPath);
                }
            } catch (IOException e) {
                // Ignore or log
            }
        }
    }

    @Override
    public String generateDownloadUrl(String key) {
        if (key == null) {
            return null;
        }
        return baseUrl + "/files/" + key;
    }

    @Override
    public byte[] getDocumentContent(String key) {
        if (key == null || key.contains("..")) return null;
        Path targetPath = Paths.get(basePath, key).normalize();
        if (!targetPath.startsWith(Paths.get(basePath).normalize())) {
            return null;
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getDocumentContentType(String key) {
        if (key == null || key.contains("..")) return null;
        Path targetPath = Paths.get(basePath, key).normalize();
        try {
            String type = Files.probeContentType(targetPath);
            if (type == null) {
                String lowerName = targetPath.toString().toLowerCase();
                if (lowerName.endsWith(".pdf")) return "application/pdf";
                if (lowerName.endsWith(".png")) return "image/png";
                if (lowerName.endsWith(".webp")) return "image/webp";
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
                return "application/octet-stream";
            }
            return type;
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
