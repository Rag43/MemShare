package com.rag.JwtLearn.media;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rag.JwtLearn.memory.Memory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String fileType; // MIME type (e.g., image/jpeg, video/mp4)
    
    @Column(nullable = false)
    private String s3Key; // S3 object key
    
    @Column(nullable = false)
    private String s3Bucket;
    
    @Column(nullable = false)
    private Long fileSize; // File size in bytes
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty("mediaType")
    private MediaType mediaType; // IMAGE, VIDEO, AUDIO, DOCUMENT
    
    @Column
    private String contentType; // Additional content type info
    
    @Column
    private Integer width; // For images/videos
    
    @Column
    private Integer height; // For images/videos
    
    @Column
    private Long duration; // For videos/audio in milliseconds
    
    @Column
    private String thumbnailS3Key; // For video thumbnails
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_id", nullable = false)
    @JsonIgnore
    private Memory memory;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper method to get full S3 URL
    public String getS3Url() {
        return String.format("https://%s.s3.amazonaws.com/%s", s3Bucket, s3Key);
    }
    
    // Helper method to get thumbnail URL
    public String getThumbnailUrl() {
        if (thumbnailS3Key != null && !thumbnailS3Key.isEmpty()) {
            return String.format("https://%s.s3.amazonaws.com/%s", s3Bucket, thumbnailS3Key);
        }
        return null;
    }
    
    // Enum for media types
    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }
}
