package com.vrtechnologies.vrtech.service;

import com.cloudinary.Cloudinary;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean configured;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = cloudinary;
        this.configured = hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    public MediaUploadResponse uploadImage(MultipartFile file, String folder) {
        String contentType = file != null ? file.getContentType() : null;
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed here");
        }

        return upload(file, folder, "image", "Image file is required", "Failed to upload image");
    }

    public MediaUploadResponse uploadMedia(MultipartFile file, String folder) {
        return upload(file, folder, "auto", "Media file is required", "Failed to upload media");
    }

    public boolean isConfigured() {
        return configured;
    }

    public void deleteAsset(String publicId) {
        deleteAsset(publicId, "image");
    }

    public void deleteAsset(String publicId, String resourceType) {
        String resolvedResourceType = resourceType == null || resourceType.isBlank() ? "image" : resourceType;
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        ensureConfigured();
        try {
            cloudinary.uploader().destroy(publicId, Map.of("resource_type", resolvedResourceType));
        } catch (IOException | RuntimeException exception) {
            throw new BadRequestException("Failed to delete media");
        }
    }

    private MediaUploadResponse upload(
            MultipartFile file,
            String folder,
            String resourceType,
            String emptyMessage,
            String failureMessage
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(emptyMessage);
        }

        ensureConfigured();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "folder", folder,
                            "resource_type", resourceType
                    )
            );

            return MediaUploadResponse.builder()
                    .url((String) result.get("secure_url"))
                    .publicId((String) result.get("public_id"))
                    .resourceType((String) result.get("resource_type"))
                    .build();
        } catch (IOException | RuntimeException exception) {
            throw new BadRequestException(failureMessage);
        }
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new BadRequestException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
