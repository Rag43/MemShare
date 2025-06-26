package com.rag.JwtLearn.media;

import com.rag.JwtLearn.memory.Memory;
import com.rag.JwtLearn.memory.MemoryRepository;
import com.rag.JwtLearn.config.JWTService;
import com.rag.JwtLearn.user.User;
import com.rag.JwtLearn.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MediaController {
    
    private final MediaService mediaService;
    private final S3Service s3Service;
    private final JWTService jwtService;
    private final UserRepository userRepository;
    
    /**
     * Upload file to a memory
     */
    @PostMapping("/upload/{memoryId}")
    public ResponseEntity<Media> uploadFile(
            @PathVariable Long memoryId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            log.info("Uploading file for memory: {}", memoryId);
            
            Long userId = getUserIdFromAuthentication(authentication);
            
            Media media = mediaService.uploadFileToMemory(file, memoryId, userId);
            log.info("File uploaded successfully: {}", media.getFileName());
            return ResponseEntity.ok(media);
            
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload multiple files to a memory
     */
    @PostMapping("/upload-multiple/{memoryId}")
    public ResponseEntity<List<Media>> uploadMultipleFiles(
            @PathVariable Long memoryId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            List<Media> uploadedMedia = mediaService.uploadFilesToMemory(files, memoryId, userId);
            return ResponseEntity.ok(uploadedMedia);
            
        } catch (Exception e) {
            log.error("Error uploading multiple files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload video with thumbnail
     */
    @PostMapping("/upload-video/{memoryId}")
    public ResponseEntity<Media> uploadVideoWithThumbnail(
            @PathVariable Long memoryId,
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnailFile,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Media uploadedVideo = mediaService.uploadVideoWithThumbnail(videoFile, thumbnailFile, memoryId, userId);
            return ResponseEntity.ok(uploadedVideo);
            
        } catch (Exception e) {
            log.error("Error uploading video: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all media for a memory
     */
    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<List<Media>> getMediaByMemoryId(
            @PathVariable Long memoryId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            List<Media> mediaList = mediaService.getMediaByMemoryId(memoryId, userId);
            return ResponseEntity.ok(mediaList);
            
        } catch (Exception e) {
            log.error("Error getting media for memory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get media by ID
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<Media> getMediaById(
            @PathVariable Long mediaId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Media media = mediaService.getMediaById(mediaId, userId);
            return ResponseEntity.ok(media);
            
        } catch (Exception e) {
            log.error("Error getting media: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Download file
     */
    @GetMapping("/download/{mediaId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long mediaId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Media media = mediaService.getMediaById(mediaId, userId);
            InputStream inputStream = mediaService.downloadFile(mediaId, userId);
            
            InputStreamResource resource = new InputStreamResource(inputStream);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(media.getFileType()));
            headers.setContentDispositionFormData("attachment", media.getOriginalFileName());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate presigned URL for file access
     */
    @GetMapping("/presigned-url/{mediaId}")
    public ResponseEntity<String> generatePresignedUrl(
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "60") long expirationInMinutes,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            String presignedUrl = mediaService.generatePresignedUrl(mediaId, userId, expirationInMinutes);
            return ResponseEntity.ok(presignedUrl);
            
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete media file
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable Long mediaId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            mediaService.deleteMedia(mediaId, userId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting media: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete all media for a memory
     */
    @DeleteMapping("/memory/{memoryId}")
    public ResponseEntity<Void> deleteAllMediaForMemory(
            @PathVariable Long memoryId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            mediaService.deleteAllMediaForMemory(memoryId, userId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting all media for memory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get media statistics for a memory
     */
    @GetMapping("/statistics/{memoryId}")
    public ResponseEntity<MediaService.MediaStatistics> getMediaStatistics(
            @PathVariable Long memoryId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            MediaService.MediaStatistics statistics = mediaService.getMediaStatistics(memoryId, userId);
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("Error getting media statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check if file exists
     */
    @GetMapping("/exists/{s3Key}")
    public ResponseEntity<Boolean> fileExists(@PathVariable String s3Key) {
        try {
            boolean exists = mediaService.fileExists(s3Key);
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cleanup orphaned media (admin only)
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Void> cleanupOrphanedMedia(Authentication authentication) {
        try {
            // TODO: Add admin role check
            mediaService.cleanupOrphanedMedia();
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error cleaning up orphaned media: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Test S3 connection (no authentication required)
     */
    @GetMapping("/test-s3-simple")
    public ResponseEntity<String> testS3ConnectionSimple() {
        try {
            log.info("=== S3 CONNECTION TEST START ===");
            log.info("Testing S3 connection with bucket: {}", s3Service.getBucketName());
            log.info("S3 region: {}", s3Service.getRegion());
            
            // Skip bucket listing test since user has explicit deny for s3:ListAllMyBuckets
            log.info("Skipping bucket listing test due to explicit deny policy");
            
            // Test: Try to access the specific bucket
            try {
                log.info("Testing bucket access...");
                boolean bucketExists = s3Service.fileExists("test-key-that-does-not-exist");
                log.info("Bucket access successful - bucket exists and is accessible");
            } catch (Exception e) {
                log.error("Bucket access failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("S3 bucket access failed: " + e.getMessage());
            }
            
            log.info("=== S3 CONNECTION TEST END ===");
            return ResponseEntity.ok("S3 connection successful! Bucket access confirmed.");
            
        } catch (Exception e) {
            log.error("=== S3 CONNECTION TEST ERROR ===");
            log.error("S3 connection test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("S3 connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Test file upload to S3 only (no authentication required)
     */
    @PostMapping("/test-upload-simple")
    public ResponseEntity<String> testFileUploadSimple(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Testing file upload without authentication");
            log.info("File details: name={}, size={}, contentType={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            // Test S3 upload using the existing service method with dummy IDs
            Media media = s3Service.uploadFile(file, 999L, 1L); // Use dummy memory ID and user ID
            
            log.info("Test file uploaded successfully to S3: {}", media.getS3Key());
            return ResponseEntity.ok("Test file uploaded successfully to S3: " + media.getS3Key());
            
        } catch (Exception e) {
            log.error("Test file upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Test file upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Temporary test endpoint with hardcoded user ID (for testing)
     */
    @PostMapping("/test-upload-temp/{memoryId}")
    public ResponseEntity<Media> testUploadWithTempUser(
            @PathVariable Long memoryId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Testing file upload with temporary user ID for memory: {}", memoryId);
            
            // Use hardcoded user ID for testing
            Long tempUserId = 1L;
            Media media = mediaService.uploadFileToMemory(file, memoryId, tempUserId);
            
            log.info("Test upload successful: {}", media.getFileName());
            return ResponseEntity.ok(media);
            
        } catch (Exception e) {
            log.error("Test upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload file without authentication (for testing)
     */
    @PostMapping("/test-upload/{memoryId}")
    public ResponseEntity<Media> uploadFileTest(
            @PathVariable Long memoryId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Uploading file without authentication for memory: {}", memoryId);
            
            // Use a default user (ID 1) for testing
            Long defaultUserId = 1L;
            
            Media media = mediaService.uploadFileToMemory(file, memoryId, defaultUserId);
            log.info("File uploaded successfully: {}", media.getFileName());
            return ResponseEntity.ok(media);
            
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Test JWT token processing (requires authentication)
     */
    @GetMapping("/test-auth")
    public ResponseEntity<String> testAuth(Authentication authentication) {
        try {
            log.info("Testing authentication");
            log.info("Authentication object: {}", authentication);
            log.info("Authentication name: {}", authentication.getName());
            log.info("Authentication principal: {}", authentication.getPrincipal());
            log.info("Authentication authorities: {}", authentication.getAuthorities());
            
            return ResponseEntity.ok("Authentication test successful! User: " + authentication.getName());
            
        } catch (Exception e) {
            log.error("Authentication test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authentication test failed: " + e.getMessage());
        }
    }
    
    /**
     * Simple test endpoint (no authentication required)
     */
    @GetMapping("/test-simple")
    public ResponseEntity<String> testSimple() {
        return ResponseEntity.ok("Simple test endpoint working!");
    }
    
    /**
     * Test file upload with detailed logging
     */
    @PostMapping("/test-upload-debug/{memoryId}")
    public ResponseEntity<String> testUploadDebug(
            @PathVariable Long memoryId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            log.info("=== DEBUG UPLOAD START ===");
            log.info("Memory ID: {}", memoryId);
            log.info("File: name={}, size={}, contentType={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            if (authentication != null) {
                log.info("Authentication: name={}, principal={}", 
                        authentication.getName(), authentication.getPrincipal());
            } else {
                log.warn("No authentication provided");
            }
            
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User ID: {}", userId);
            
            Media media = mediaService.uploadFileToMemory(file, memoryId, userId);
            log.info("Upload successful: mediaId={}, fileName={}, s3Key={}", 
                    media.getId(), media.getFileName(), media.getS3Key());
            
            log.info("=== DEBUG UPLOAD END ===");
            return ResponseEntity.ok("Upload successful: " + media.getFileName());
            
        } catch (Exception e) {
            log.error("=== DEBUG UPLOAD ERROR ===");
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Simple S3 upload test (no authentication, no database)
     */
    @PostMapping("/test-s3-upload-only")
    public ResponseEntity<String> testS3UploadOnly(@RequestParam("file") MultipartFile file) {
        try {
            log.info("=== S3 UPLOAD TEST START ===");
            log.info("File: name={}, size={}, contentType={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            log.info("S3 config: bucket={}, region={}", 
                    s3Service.getBucketName(), s3Service.getRegion());
            
            // Test direct S3 upload without database
            String s3Key = "test-uploads/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Service.getBucketName())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();
            
            log.info("Uploading to S3 key: {}", s3Key);
            s3Service.getS3Client().putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("S3 upload successful!");
            log.info("=== S3 UPLOAD TEST END ===");
            
            return ResponseEntity.ok("S3 upload successful! Key: " + s3Key);
            
        } catch (Exception e) {
            log.error("=== S3 UPLOAD TEST ERROR ===");
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("S3 upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Test AWS credentials (no S3 operations)
     */
    @GetMapping("/test-credentials")
    public ResponseEntity<String> testCredentials() {
        try {
            log.info("=== CREDENTIALS TEST START ===");
            
            // Just check if the S3 client was created successfully
            var s3Client = s3Service.getS3Client();
            log.info("S3 client created successfully");
            log.info("S3 client configuration: {}", s3Client.serviceClientConfiguration());
            
            log.info("=== CREDENTIALS TEST END ===");
            return ResponseEntity.ok("S3 client created successfully. Configuration loaded.");
            
        } catch (Exception e) {
            log.error("=== CREDENTIALS TEST ERROR ===");
            log.error("Credentials test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Credentials test failed: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required");
        }
        
        // Use email to find user ID (simple and reliable approach)
        String email = authentication.getName();
        log.debug("Using email to find user: {}", email);
        
        try {
            // Find user by email and return ID
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getId().longValue();
        } catch (Exception e) {
            log.error("Error finding user by email: {}", e.getMessage());
            throw new RuntimeException("Failed to get user ID from authentication");
        }
    }
}
