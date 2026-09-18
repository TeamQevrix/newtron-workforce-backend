package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.service.StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{dir}/{filename}")
    public ResponseEntity<byte[]> getFile(
            @PathVariable("dir") String dir,
            @PathVariable("filename") String filename) {
        
        String key = dir + "/" + filename;
        byte[] content = storageService.getDocumentContent(key);
        
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        String contentType = storageService.getDocumentContentType(key);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
