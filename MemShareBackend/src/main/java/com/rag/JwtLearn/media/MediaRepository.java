package com.rag.JwtLearn.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    
    /**
     * Find all media by memory ID
     */
    List<Media> findByMemoryIdOrderByCreatedAtAsc(Long memoryId);
    
    /**
     * Find all media by memory ID and media type
     */
    List<Media> findByMemoryIdAndMediaTypeOrderByCreatedAtAsc(Long memoryId, Media.MediaType mediaType);
    
    /**
     * Find all images by memory ID
     */
    @Query("SELECT m FROM Media m WHERE m.memory.id = :memoryId AND m.mediaType = 'IMAGE' ORDER BY m.createdAt ASC")
    List<Media> findImagesByMemoryId(@Param("memoryId") Long memoryId);
    
    /**
     * Find all videos by memory ID
     */
    @Query("SELECT m FROM Media m WHERE m.memory.id = :memoryId AND m.mediaType = 'VIDEO' ORDER BY m.createdAt ASC")
    List<Media> findVideosByMemoryId(@Param("memoryId") Long memoryId);
    
    /**
     * Find media by S3 key
     */
    Media findByS3Key(String s3Key);
    
    /**
     * Check if media exists by S3 key
     */
    boolean existsByS3Key(String s3Key);
    
    /**
     * Find all media by user ID
     */
    @Query("SELECT m FROM Media m WHERE m.memory.user.id = :userId ORDER BY m.createdAt DESC")
    List<Media> findByUserId(@Param("userId") Long userId);
    
    /**
     * Count media by memory ID
     */
    long countByMemoryId(Long memoryId);
    
    /**
     * Count media by memory ID and media type
     */
    long countByMemoryIdAndMediaType(Long memoryId, Media.MediaType mediaType);
    
    /**
     * Find media with file size greater than specified size
     */
    @Query("SELECT m FROM Media m WHERE m.fileSize > :minSize ORDER BY m.fileSize DESC")
    List<Media> findLargeFiles(@Param("minSize") Long minSize);
    
    /**
     * Find orphaned media (media without associated memory)
     */
    @Query("SELECT m FROM Media m WHERE m.memory IS NULL")
    List<Media> findOrphanedMedia();
} 