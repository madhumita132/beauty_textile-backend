package com.beautytextile.service.storage;

import org.springframework.web.multipart.MultipartFile;

/** Abstraction for image storage — swap Local ↔ S3 ↔ GCS ↔ Azure without changing callers. */
public interface ImageStorageService {
    /**
     * Store a file and return the public URL path.
     * @return e.g. /images/uuid.jpg  (local)  or  https://bucket.s3.../key.jpg  (cloud)
     */
    String store(MultipartFile file);

    void delete(String fileUrl);
}
