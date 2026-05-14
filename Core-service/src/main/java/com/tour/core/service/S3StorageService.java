package com.tour.core.service;

import com.tour.core.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${aws.s3.region:ap-southeast-1}")
    private String region;

    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("File ảnh không được trống");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new InvalidDataException("AWS S3 bucket chưa được cấu hình");
        }

        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = folder + "/" + UUID.randomUUID() + "-" + safeName;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException ex) {
            throw new InvalidDataException("Không thể đọc file ảnh để upload");
        }

        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    public void deleteByUrl(String imageUrl) {
        String key = extractKey(imageUrl);
        if (key == null || key.isBlank() || bucketName == null || bucketName.isBlank()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    private String extractKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String cleanedUrl = imageUrl.split("\\?")[0];
        if (publicBaseUrl != null && !publicBaseUrl.isBlank() && cleanedUrl.startsWith(publicBaseUrl)) {
            return cleanedUrl.substring(publicBaseUrl.length()).replaceFirst("^/", "");
        }

        try {
            URI uri = new URI(cleanedUrl);
            String path = uri.getPath();
            return path == null ? null : path.replaceFirst("^/", "");
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}