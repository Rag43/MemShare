package com.rag.JwtLearn.memory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rag.JwtLearn.media.Media;
import com.rag.JwtLearn.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "memories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Memory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // The main text content of the memory
    
    @Column(nullable = false)
    private String title; // Optional title for the memory
    
    @Column(name = "memory_date", nullable = false)
    private LocalDateTime memoryDate; // When the memory occurred
    
    @Column(name = "location")
    private String location; // Optional location where the memory occurred
    
    @Column(name = "is_public")
    private Boolean isPublic = false; // Whether the memory is public or private
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user; // The user who created this memory
    
    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Media> media = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods for managing media
    public void addMedia(Media mediaItem) {
        if (media == null) {
            media = new ArrayList<>();
        }
        media.add(mediaItem);
        mediaItem.setMemory(this);
    }
    
    public void removeMedia(Media mediaItem) {
        if (media != null) {
            media.remove(mediaItem);
            mediaItem.setMemory(null);
        }
    }
    
    // Helper method to get only images
    public List<Media> getImages() {
        if (media == null) {
            return new ArrayList<>();
        }
        return media.stream()
                .filter(m -> m.getMediaType() == Media.MediaType.IMAGE)
                .toList();
    }
    
    // Helper method to get only videos
    public List<Media> getVideos() {
        if (media == null) {
            return new ArrayList<>();
        }
        return media.stream()
                .filter(m -> m.getMediaType() == Media.MediaType.VIDEO)
                .toList();
    }
    
    // Helper method to check if memory has media
    public boolean hasMedia() {
        return media != null && !media.isEmpty();
    }
    
    // Helper method to get media count
    public int getMediaCount() {
        return media != null ? media.size() : 0;
    }
}
