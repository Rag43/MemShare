package com.rag.JwtLearn.memory.dto;

import com.rag.JwtLearn.media.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime memoryDate;
    private String location;
    private Boolean isPublic;
    private String displayPic;
    private Integer userId; // User ID for authorization checks
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Media> media;
} 