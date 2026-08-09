package com.newtron.newtron_workforce_backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadProfilePhoto(MultipartFile file);
    String uploadDocument(MultipartFile file);
    void deletePhoto(String key);
    String generateDownloadUrl(String key);
    byte[] getDocumentContent(String key);
    String getDocumentContentType(String key);
}
