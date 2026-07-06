package com.beautytextile.service.storage;

import com.beautytextile.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseImageStorageService implements ImageStorageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String projectUrl;
    private final String serviceKey;
    private final String bucket;
    private final String publicBase;

    public SupabaseImageStorageService(
            @Value("${app.storage.supabase.project-url}") String projectUrl,
            @Value("${app.storage.supabase.service-key}") String serviceKey,
            @Value("${app.storage.supabase.bucket}") String bucket,
            @Value("${app.storage.supabase.public-base-url:}") String publicBaseUrl
    ) {
        if (isBlank(projectUrl) || isBlank(serviceKey) || isBlank(bucket)) {
            throw new IllegalStateException("Supabase is enabled but project-url/service-key/bucket is missing");
        }
        this.projectUrl = trimTrailingSlash(projectUrl);
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.publicBase = isBlank(publicBaseUrl)
                ? this.projectUrl + "/storage/v1/object/public/" + bucket
                : trimTrailingSlash(publicBaseUrl);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Image file is empty");
        }
        try {
            String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "jpg";
            String objectPath = "products/" + UUID.randomUUID() + "." + ext;
            String encodedObjectPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
            String uploadUrl = projectUrl + "/storage/v1/object/" + bucket + "/" + encodedObjectPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("x-upsert", "true");

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException("Failed to upload image to Supabase: " + response.getStatusCode());
            }
            return publicBase + "/" + objectPath;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            Throwable root = e.getMostSpecificCause();
            if (root instanceof UnknownHostException) {
                throw new BusinessException("Failed to upload image to Supabase: cannot resolve host. Check SUPABASE_PROJECT_URL");
            }
            throw new BusinessException("Failed to upload image to Supabase: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("Failed to upload image to Supabase: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (isBlank(fileUrl)) {
            return;
        }
        try {
            String objectPath = extractObjectPath(fileUrl);
            if (isBlank(objectPath)) return;
            int query = objectPath.indexOf('?');
            if (query >= 0) objectPath = objectPath.substring(0, query);
            String encodedObjectPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
            String deleteUrl = projectUrl + "/storage/v1/object/" + bucket + "/" + encodedObjectPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
        } catch (Exception ignored) {
            // Deletion should not break business flow.
        }
    }

    private String extractObjectPath(String fileUrl) {
        String apiMarker = "/storage/v1/object/public/" + bucket + "/";
        int apiIdx = fileUrl.indexOf(apiMarker);
        if (apiIdx >= 0) {
            return fileUrl.substring(apiIdx + apiMarker.length());
        }

        String normalizedPublicBase = trimTrailingSlash(publicBase);
        String normalizedUrl = trimTrailingSlash(fileUrl);
        if (normalizedUrl.startsWith(normalizedPublicBase + "/")) {
            return normalizedUrl.substring(normalizedPublicBase.length() + 1);
        }
        return null;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
