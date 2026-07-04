package com.beautytextile.service;

import com.beautytextile.service.storage.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Thin facade kept for backward-compatibility.
 * Delegates to ImageStorageService (local disk by default; swap to cloud).
 */
@Service
public class FileStorageService {

    private final ImageStorageService storage;

    public FileStorageService(ImageStorageService storage) {
        this.storage = storage;
    }

    public String store(MultipartFile file) {
        return storage.store(file);
    }
}
