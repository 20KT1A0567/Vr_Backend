package com.vrtechnologies.vrtech.service;

import com.cloudinary.Cloudinary;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
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

    public void deleteAsset(String publicId) {
        deleteAsset(publicId, "image");
    }

    public void deleteAsset(String publicId, String resourceType) {
        String resolvedResourceType = resourceType == null || resourceType.isBlank() ? "image" : resourceType;
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, Map.of("resource_type", resolvedResourceType));
        } catch (IOException exception) {
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
        } catch (IOException exception) {
            throw new BadRequestException(failureMessage);
        }
    }
}
