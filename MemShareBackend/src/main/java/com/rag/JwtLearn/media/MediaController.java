package com.rag.JwtLearn.media;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {
    
    private final MediaService mediaService;
    
    /**
     * Upload single file to a memory
     */
    @PostMapping("/upload/{memoryId}")
    public ResponseEntity<Media> uploadFile(
            @PathVariable Long memoryId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Media uploadedMedia = mediaService.uploadFileToMemory(file, memoryId, userId);
            return ResponseEntity.ok(uploadedMedia);
            
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
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
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // TODO: Implement based on your authentication setup
        // This is a placeholder - you'll need to implement this based on your JWT setup
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required");
        }
        
        // Assuming your JWT contains user ID in the principal
        // You might need to adjust this based on your JWT implementation
        String principal = authentication.getPrincipal().toString();
        
        // This is a simplified example - you should implement proper user ID extraction
        // from your JWT token or user details
        try {
            // Extract user ID from JWT or user details
            // For now, returning a placeholder
            return 1L; // Replace with actual user ID extraction
        } catch (Exception e) {
            throw new RuntimeException("Invalid user authentication");
        }
    }
}
