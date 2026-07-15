package com.beautytextile.service.storage;

import com.beautytextile.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseImageStorageService.class);

    private static final String HTTPS_PREFIX = "https://";
    private static final String HTTP_PREFIX = "http://";
    private static final String PUBLIC_OBJECT_PATH = "/storage/v1/object/public/";
    private static final String S3_PATH_SUFFIX = "/storage/v1/s3";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String projectUrl;
    private final String serviceKey;
    private final String bucket;
    private final String publicBase;
    private final String projectHost;

    public SupabaseImageStorageService(
            @Value("${app.storage.supabase.project-url}") String projectUrl,
            @Value("${app.storage.supabase.service-key}") String serviceKey,
            @Value("${app.storage.supabase.bucket}") String bucket,
            @Value("${app.storage.supabase.public-base-url:}") String publicBaseUrl
    ) {
        projectUrl = normalizeValue(projectUrl);
        serviceKey = normalizeValue(serviceKey);
        bucket = normalizeValue(bucket);
        publicBaseUrl = normalizeValue(publicBaseUrl);

        if (isBlank(projectUrl) || isBlank(serviceKey) || isBlank(bucket)) {
            throw new IllegalStateException("Supabase is enabled but project-url/service-key/bucket is missing");
        }
        this.projectUrl = normalizeProjectUrl(projectUrl);
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.publicBase = isBlank(publicBaseUrl)
            ? this.projectUrl + PUBLIC_OBJECT_PATH + bucket
                : trimTrailingSlash(publicBaseUrl);
        this.projectHost = extractHost(this.projectUrl);

        log.info("Supabase storage initialized: provider=supabase, projectUrl={}, projectHost={}, bucket={}, publicBase={}",
            this.projectUrl, this.projectHost, this.bucket, this.publicBase);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Image file is empty");
        }
        try {
            String original = file.getOriginalFilename();
            if (original == null) {
                original = "image";
            }
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "jpg";
            String objectPath = "products/" + UUID.randomUUID() + "." + ext;
            String encodedObjectPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
            String uploadUrl = projectUrl + "/storage/v1/object/" + bucket + "/" + encodedObjectPath;

                log.info("Supabase upload start: host={}, bucket={}, objectPath={}, contentType={}, sizeBytes={}",
                    projectHost, bucket, objectPath, file.getContentType(), file.getSize());

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("x-upsert", "true");

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Supabase upload non-success status: status={}, host={}, bucket={}, objectPath={}",
                        response.getStatusCode(), projectHost, bucket, objectPath);
                throw new BusinessException("Failed to upload image to Supabase: " + response.getStatusCode());
            }
            log.info("Supabase upload success: host={}, bucket={}, objectPath={}", projectHost, bucket, objectPath);
            return publicBase + "/" + objectPath;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            Throwable root = e.getMostSpecificCause();
            if (root instanceof UnknownHostException) {
                log.error("Supabase upload DNS failure: projectUrl={}, host={}, message={}",
                        projectUrl, projectHost, root.getMessage());
                throw new BusinessException("Failed to upload image to Supabase: cannot resolve host. Check SUPABASE_PROJECT_URL");
            }
            log.error("Supabase upload resource access failure: projectUrl={}, host={}, message={}",
                    projectUrl, projectHost, e.getMessage());
            throw new BusinessException("Failed to upload image to Supabase: " + e.getMessage());
        } catch (Exception e) {
            log.error("Supabase upload failed: projectUrl={}, host={}, message={}",
                    projectUrl, projectHost, e.getMessage());
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
            if (isBlank(objectPath)) {
                log.warn("Supabase delete skipped: cannot extract objectPath from fileUrl={}", fileUrl);
                return;
            }
            int query = objectPath.indexOf('?');
            if (query >= 0) objectPath = objectPath.substring(0, query);
            String encodedObjectPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
            String deleteUrl = projectUrl + "/storage/v1/object/" + bucket + "/" + encodedObjectPath;

            log.info("Supabase delete start: host={}, bucket={}, objectPath={}", projectHost, bucket, objectPath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", serviceKey);
            headers.set("Authorization", "Bearer " + serviceKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
            log.info("Supabase delete success: host={}, bucket={}, objectPath={}", projectHost, bucket, objectPath);
        } catch (Exception ignored) {
            // Deletion should not break business flow.
            log.warn("Supabase delete failed (ignored): host={}, bucket={}, message={}",
                    projectHost, bucket, ignored.getMessage());
        }
    }

    private String extractObjectPath(String fileUrl) {
        String apiMarker = PUBLIC_OBJECT_PATH + bucket + "/";
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

    private String normalizeProjectUrl(String rawUrl) {
        String url = rawUrl;
        if (!url.startsWith(HTTP_PREFIX) && !url.startsWith(HTTPS_PREFIX)) {
            url = HTTPS_PREFIX + url;
        }
        url = trimTrailingSlash(url);

        if (url.contains(PUBLIC_OBJECT_PATH)) {
            int idx = url.indexOf(PUBLIC_OBJECT_PATH);
            return url.substring(0, idx);
        }

        if (url.endsWith(S3_PATH_SUFFIX)) {
            String withoutSuffix = url.substring(0, url.length() - S3_PATH_SUFFIX.length());
            return trimTrailingSlash(withoutSuffix);
        }

        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && host.endsWith(".storage.supabase.co")) {
                String projectRef = host.substring(0, host.indexOf(".storage.supabase.co"));
                return HTTPS_PREFIX + projectRef + ".supabase.co";
            }
        } catch (Exception ignored) {
            // Let downstream upload attempt produce a clear error if URL is still malformed.
        }
        return url;
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeValue(String value) {
        if (value == null) return null;
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }
}
