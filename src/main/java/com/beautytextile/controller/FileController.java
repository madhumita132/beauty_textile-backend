package com.beautytextile.controller;

import com.beautytextile.service.storage.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * POST /api/files/upload — upload product image.
 * Response: { "fileName": "...", "fileUrl": "/images/..." }
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final ImageStorageService storage;

    public FileController(ImageStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String url = storage.store(file);
        String name = url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : url;
        return Map.of("fileName", name, "fileUrl", url);
    }
}
