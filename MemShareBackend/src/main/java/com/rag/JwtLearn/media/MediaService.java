package com.rag.JwtLearn.media;

import com.rag.JwtLearn.memory.Memory;
import com.rag.JwtLearn.memory.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MediaService {
    
    private final MediaRepository mediaRepository;
    private final MemoryRepository memoryRepository;
    private final S3Service s3Service;
    
    /**
     * Upload single file to a memory
     */
    public Media uploadFileToMemory(MultipartFile file, Long memoryId, Long userId) throws IOException {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        // Upload file to S3
        Media media = s3Service.uploadFile(file, memoryId, userId);
        media.setMemory(memory);
        
        // Save to database
        Media savedMedia = mediaRepository.save(media);
        
        log.info("File uploaded successfully for memory {}: {}", memoryId, savedMedia.getFileName());
        return savedMedia;
    }
    
    /**
     * Upload multiple files to a memory
     */
    public List<Media> uploadFilesToMemory(List<MultipartFile> files, Long memoryId, Long userId) throws IOException {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        List<Media> uploadedMedia = new java.util.ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Media media = s3Service.uploadFile(file, memoryId, userId);
                media.setMemory(memory);
                Media savedMedia = mediaRepository.save(media);
                uploadedMedia.add(savedMedia);
            }
        }
        
        log.info("Uploaded {} files for memory {}", uploadedMedia.size(), memoryId);
        return uploadedMedia;
    }
    
    /**
     * Upload video with thumbnail
     */
    public Media uploadVideoWithThumbnail(MultipartFile videoFile, MultipartFile thumbnailFile, 
                                        Long memoryId, Long userId) throws IOException {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        // Upload video with thumbnail to S3
        Media media = s3Service.uploadVideoWithThumbnail(videoFile, thumbnailFile, memoryId, userId);
        media.setMemory(memory);
        
        // Save to database
        Media savedMedia = mediaRepository.save(media);
        
        log.info("Video uploaded successfully for memory {}: {}", memoryId, savedMedia.getFileName());
        return savedMedia;
    }
    
    /**
     * Get all media for a memory
     */
    @Transactional(readOnly = true)
    public List<Media> getMediaByMemoryId(Long memoryId, Long userId) {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        return mediaRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId);
    }
    
    /**
     * Get media by ID
     */
    @Transactional(readOnly = true)
    public Media getMediaById(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        
        if (!media.getMemory().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to media");
        }
        
        return media;
    }
    
    /**
     * Download file
     */
    @Transactional(readOnly = true)
    public InputStream downloadFile(Long mediaId, Long userId) {
        Media media = getMediaById(mediaId, userId);
        return s3Service.downloadFile(media.getS3Key());
    }
    
    /**
     * Generate presigned URL for file access
     */
    @Transactional(readOnly = true)
    public String generatePresignedUrl(Long mediaId, Long userId, long expirationInMinutes) {
        Media media = getMediaById(mediaId, userId);
        return s3Service.generatePresignedUrl(media.getS3Key(), expirationInMinutes);
    }
    
    /**
     * Delete media file
     */
    public void deleteMedia(Long mediaId, Long userId) {
        Media media = getMediaById(mediaId, userId);
        
        // Delete from S3
        s3Service.deleteFile(media.getS3Key());
        
        // Delete thumbnail if exists
        if (media.getThumbnailS3Key() != null && !media.getThumbnailS3Key().isEmpty()) {
            s3Service.deleteFile(media.getThumbnailS3Key());
        }
        
        // Delete from database
        mediaRepository.delete(media);
        
        log.info("Media deleted successfully: {}", mediaId);
    }
    
    /**
     * Delete all media for a memory
     */
    public void deleteAllMediaForMemory(Long memoryId, Long userId) {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        List<Media> mediaList = mediaRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId);
        
        // Collect S3 keys for batch deletion
        List<String> s3Keys = new java.util.ArrayList<>();
        List<String> thumbnailKeys = new java.util.ArrayList<>();
        
        for (Media media : mediaList) {
            s3Keys.add(media.getS3Key());
            if (media.getThumbnailS3Key() != null && !media.getThumbnailS3Key().isEmpty()) {
                thumbnailKeys.add(media.getThumbnailS3Key());
            }
        }
        
        // Delete from S3
        if (!s3Keys.isEmpty()) {
            s3Service.deleteFiles(s3Keys);
        }
        if (!thumbnailKeys.isEmpty()) {
            s3Service.deleteFiles(thumbnailKeys);
        }
        
        // Delete from database
        mediaRepository.deleteAll(mediaList);
        
        log.info("Deleted {} media files for memory {}", mediaList.size(), memoryId);
    }
    
    /**
     * Get media statistics for a memory
     */
    @Transactional(readOnly = true)
    public MediaStatistics getMediaStatistics(Long memoryId, Long userId) {
        // Verify memory exists and belongs to user
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        if (!memory.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        long totalCount = mediaRepository.countByMemoryId(memoryId);
        long imageCount = mediaRepository.countByMemoryIdAndMediaType(memoryId, Media.MediaType.IMAGE);
        long videoCount = mediaRepository.countByMemoryIdAndMediaType(memoryId, Media.MediaType.VIDEO);
        long audioCount = mediaRepository.countByMemoryIdAndMediaType(memoryId, Media.MediaType.AUDIO);
        long documentCount = mediaRepository.countByMemoryIdAndMediaType(memoryId, Media.MediaType.DOCUMENT);
        
        return MediaStatistics.builder()
                .totalCount(totalCount)
                .imageCount(imageCount)
                .videoCount(videoCount)
                .audioCount(audioCount)
                .documentCount(documentCount)
                .build();
    }
    
    /**
     * Check if file exists in S3
     */
    @Transactional(readOnly = true)
    public boolean fileExists(String s3Key) {
        return s3Service.fileExists(s3Key);
    }
    
    /**
     * Find orphaned media and clean up
     */
    public void cleanupOrphanedMedia() {
        List<Media> orphanedMedia = mediaRepository.findOrphanedMedia();
        
        if (!orphanedMedia.isEmpty()) {
            List<String> s3Keys = orphanedMedia.stream()
                    .map(Media::getS3Key)
                    .toList();
            
            // Delete from S3
            s3Service.deleteFiles(s3Keys);
            
            // Delete from database
            mediaRepository.deleteAll(orphanedMedia);
            
            log.info("Cleaned up {} orphaned media files", orphanedMedia.size());
        }
    }
    
    /**
     * Media statistics DTO
     */
    public static class MediaStatistics {
        private final long totalCount;
        private final long imageCount;
        private final long videoCount;
        private final long audioCount;
        private final long documentCount;
        
        public MediaStatistics(long totalCount, long imageCount, long videoCount, long audioCount, long documentCount) {
            this.totalCount = totalCount;
            this.imageCount = imageCount;
            this.videoCount = videoCount;
            this.audioCount = audioCount;
            this.documentCount = documentCount;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getImageCount() { return imageCount; }
        public long getVideoCount() { return videoCount; }
        public long getAudioCount() { return audioCount; }
        public long getDocumentCount() { return documentCount; }
        
        public static MediaStatisticsBuilder builder() {
            return new MediaStatisticsBuilder();
        }
        
        public static class MediaStatisticsBuilder {
            private long totalCount;
            private long imageCount;
            private long videoCount;
            private long audioCount;
            private long documentCount;
            
            public MediaStatisticsBuilder totalCount(long totalCount) {
                this.totalCount = totalCount;
                return this;
            }
            
            public MediaStatisticsBuilder imageCount(long imageCount) {
                this.imageCount = imageCount;
                return this;
            }
            
            public MediaStatisticsBuilder videoCount(long videoCount) {
                this.videoCount = videoCount;
                return this;
            }
            
            public MediaStatisticsBuilder audioCount(long audioCount) {
                this.audioCount = audioCount;
                return this;
            }
            
            public MediaStatisticsBuilder documentCount(long documentCount) {
                this.documentCount = documentCount;
                return this;
            }
            
            public MediaStatistics build() {
                return new MediaStatistics(totalCount, imageCount, videoCount, audioCount, documentCount);
            }
        }
    }
}
