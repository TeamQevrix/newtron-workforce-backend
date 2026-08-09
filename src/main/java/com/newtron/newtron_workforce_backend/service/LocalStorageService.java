package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalStorageService implements StorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    private static final Map<String, byte[]> FILE_CONTENTS = new ConcurrentHashMap<>();
    private static final Map<String, String> FILE_CONTENT_TYPES = new ConcurrentHashMap<>();

    @Override
    public String uploadProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FILE_EMPTY", "File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("FILE_TOO_LARGE", "File size exceeds the limit of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("INVALID_FILE_TYPE", "Only JPEG, PNG, and WEBP images are allowed");
        }

        // Simulate file upload and return a unique storage key
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "profiles/" + UUID.randomUUID().toString() + extension;
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

        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = "documents/" + UUID.randomUUID().toString() + extension;
        try {
            FILE_CONTENTS.put(key, file.getBytes());
            FILE_CONTENT_TYPES.put(key, contentType.toLowerCase());
        } catch (IOException e) {
            throw new ValidationException("FILE_READ_ERROR", "Failed to read file bytes: " + e.getMessage());
        }

        return key;
    }

    @Override
    public void deletePhoto(String key) {
        if (key != null) {
            FILE_CONTENTS.remove(key);
            FILE_CONTENT_TYPES.remove(key);
        }
    }

    @Override
    public String generateDownloadUrl(String key) {
        if (key == null) {
            return null;
        }
        return "http://localhost:8080/files/" + key;
    }

    @Override
    public byte[] getDocumentContent(String key) {
        if (key == null) return null;
        return FILE_CONTENTS.get(key);
    }

    @Override
    public String getDocumentContentType(String key) {
        if (key == null) return null;
        return FILE_CONTENT_TYPES.get(key);
    }
}
