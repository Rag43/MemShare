package com.rag.JwtLearn.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    /**
     * Upload a file to S3 and return Media metadata
     */
    public Media uploadFile(MultipartFile file, Long memoryId, Long userId) throws IOException {
        try {
            // Generate unique S3 key
            String s3Key = generateS3Key(file.getOriginalFilename(), memoryId, userId);
            
            // Upload file to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            // Create Media entity
            Media.MediaType mediaType = determineMediaType(file.getContentType());
            
            Media media = Media.builder()
                    .fileName(generateFileName(file.getOriginalFilename()))
                    .originalFileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .s3Key(s3Key)
                    .s3Bucket(bucketName)
                    .fileSize(file.getSize())
                    .mediaType(mediaType)
                    .contentType(file.getContentType())
                    .build();
            
            log.info("File uploaded successfully to S3: {}", s3Key);
            return media;
            
        } catch (S3Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    
    /**
     * Upload video with thumbnail generation
     */
    public Media uploadVideoWithThumbnail(MultipartFile videoFile, MultipartFile thumbnailFile, 
                                        Long memoryId, Long userId) throws IOException {
        try {
            // Upload video
            Media videoMedia = uploadFile(videoFile, memoryId, userId);
            
            // Upload thumbnail if provided
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbnailS3Key = generateS3Key("thumb_" + videoFile.getOriginalFilename(), memoryId, userId);
                
                PutObjectRequest thumbnailRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(thumbnailS3Key)
                        .contentType(thumbnailFile.getContentType())
                        .contentLength(thumbnailFile.getSize())
                        .build();
                
                s3Client.putObject(thumbnailRequest, 
                        RequestBody.fromInputStream(thumbnailFile.getInputStream(), thumbnailFile.getSize()));
                
                videoMedia.setThumbnailS3Key(thumbnailS3Key);
            }
            
            return videoMedia;
            
        } catch (S3Exception e) {
            log.error("Error uploading video with thumbnail: {}", e.getMessage());
            throw new RuntimeException("Failed to upload video with thumbnail", e);
        }
    }
    
    /**
     * Download file from S3
     */
    public InputStream downloadFile(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            return s3Client.getObject(getObjectRequest);
            
        } catch (S3Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }
    
    /**
     * Delete file from S3
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", s3Key);
            
        } catch (S3Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
    
    /**
     * Delete multiple files from S3
     */
    public void deleteFiles(java.util.List<String> s3Keys) {
        if (s3Keys.isEmpty()) {
            return;
        }
        
        try {
            // S3 allows up to 1000 objects per delete request
            int batchSize = 1000;
            for (int i = 0; i < s3Keys.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, s3Keys.size());
                java.util.List<String> batch = s3Keys.subList(i, endIndex);
                
                java.util.List<ObjectIdentifier> objects = batch.stream()
                        .map(key -> ObjectIdentifier.builder().key(key).build())
                        .toList();
                
                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(objects).build())
                        .build();
                
                s3Client.deleteObjects(deleteObjectsRequest);
            }
            
            log.info("Deleted {} files from S3", s3Keys.size());
            
        } catch (S3Exception e) {
            log.error("Error deleting files from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete files from S3", e);
        }
    }
    
    /**
     * Generate presigned URL for file access
     */
    public String generatePresignedUrl(String s3Key, long expirationInMinutes) {
        try {
            PresignedGetObjectRequest presignedGetObjectRequest = PresignedGetObjectRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(expirationInMinutes))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build())
                    .build();
            
            return s3Client.presignGetObject(presignedGetObjectRequest).url().toString();
            
        } catch (S3Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
    
    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            throw new RuntimeException("Failed to check file existence", e);
        }
    }
    
    /**
     * Generate unique S3 key
     */
    private String generateS3Key(String originalFilename, Long memoryId, Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uniqueId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        
        return String.format("users/%d/memories/%d/%s%s", userId, memoryId, uniqueId, extension);
    }
    
    /**
     * Generate unique filename
     */
    private String generateFileName(String originalFilename) {
        String uniqueId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        return uniqueId + extension;
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    /**
     * Determine media type from content type
     */
    private Media.MediaType determineMediaType(String contentType) {
        if (contentType == null) {
            return Media.MediaType.DOCUMENT;
        }
        
        if (contentType.startsWith("image/")) {
            return Media.MediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return Media.MediaType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return Media.MediaType.AUDIO;
        } else {
            return Media.MediaType.DOCUMENT;
        }
    }
    
    /**
     * Get file metadata from S3
     */
    public HeadObjectResponse getFileMetadata(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            return s3Client.headObject(headObjectRequest);
            
        } catch (S3Exception e) {
            log.error("Error getting file metadata: {}", e.getMessage());
            throw new RuntimeException("Failed to get file metadata", e);
        }
    }
}
