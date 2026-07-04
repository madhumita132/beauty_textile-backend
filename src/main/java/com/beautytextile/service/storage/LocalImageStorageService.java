package com.beautytextile.service.storage;

import com.beautytextile.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Local disk storage implementation.
 * Set as @Primary — replace with S3StorageService and flip @Primary when deploying to cloud.
 */
@Service
@Primary
public class LocalImageStorageService implements ImageStorageService {

    private final Path root;

    public LocalImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Image file is empty");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/images/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Failed to store image: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/images/")) return;
        try {
            Files.deleteIfExists(root.resolve(fileUrl.substring("/images/".length())));
        } catch (IOException ignored) {}
    }
}
