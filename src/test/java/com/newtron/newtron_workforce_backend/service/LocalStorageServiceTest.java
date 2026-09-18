package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService localStorageService;
    private final String tempBasePath = "./test-uploads";

    @BeforeEach
    void setUp() {
        localStorageService = new LocalStorageService();
        ReflectionTestUtils.setField(localStorageService, "basePath", tempBasePath);
        ReflectionTestUtils.setField(localStorageService, "baseUrl", "http://localhost:8080");
        localStorageService.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        Path path = Paths.get(tempBasePath);
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testUploadValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy image content".getBytes());
        String key = localStorageService.uploadProfilePhoto(file);
        
        assertNotNull(key);
        assertTrue(key.startsWith("profiles/"));
        assertTrue(Files.exists(Paths.get(tempBasePath, key)));
    }

    @Test
    void testPersistenceAfterSimulatedRestart() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy image content".getBytes());
        String key = localStorageService.uploadProfilePhoto(file);
        
        // Simulate restart
        LocalStorageService newService = new LocalStorageService();
        ReflectionTestUtils.setField(newService, "basePath", tempBasePath);
        newService.init();
        
        byte[] content = newService.getDocumentContent(key);
        assertNotNull(content);
        assertEquals("dummy image content", new String(content));
    }

    @Test
    void testUploadValidPng() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy image content".getBytes());
        String key = localStorageService.uploadProfilePhoto(file);
        assertNotNull(key);
        assertTrue(Files.exists(Paths.get(tempBasePath, key)));
    }

    @Test
    void testUploadValidWebp() {
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", "dummy image content".getBytes());
        String key = localStorageService.uploadProfilePhoto(file);
        assertNotNull(key);
        assertTrue(Files.exists(Paths.get(tempBasePath, key)));
    }

    @Test
    void testUploadPdfToDocument() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy pdf content".getBytes());
        String key = localStorageService.uploadDocument(file);
        assertNotNull(key);
        assertTrue(key.startsWith("documents/"));
        assertTrue(Files.exists(Paths.get(tempBasePath, key)));
    }

    @Test
    void testUploadTooLargeFile() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeContent);
        assertThrows(ValidationException.class, () -> localStorageService.uploadProfilePhoto(file));
    }

    @Test
    void testUploadInvalidMimeType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "text content".getBytes());
        assertThrows(ValidationException.class, () -> localStorageService.uploadProfilePhoto(file));
    }

    @Test
    void testGetNonExistentFile() {
        byte[] content = localStorageService.getDocumentContent("profiles/doesnotexist.jpg");
        assertNull(content);
    }

    @Test
    void testPathTraversalRejected() {
        byte[] content = localStorageService.getDocumentContent("../application.properties");
        assertNull(content);
    }

    @Test
    void testGenerateDownloadUrl() {
        String url = localStorageService.generateDownloadUrl("jobs/test.jpg");
        assertEquals("http://localhost:8080/files/jobs/test.jpg", url);
    }
}
