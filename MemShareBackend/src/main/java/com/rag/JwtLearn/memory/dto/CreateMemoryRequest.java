package com.rag.JwtLearn.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryRequest {
    
    @NotBlank(message = "Content is required")
    @Size(max = 500, message = "Content cannot exceed 500 characters")
    private String content;
    
    @NotBlank(message = "Title is required")
    @Size(max = 50, message = "Title cannot exceed 50 characters")
    private String title;
    
    private LocalDateTime memoryDate = LocalDateTime.now(); // Default to current time
    
    @Size(max = 50, message = "Location cannot exceed 50 characters")
    private String location;
    
    private Boolean isPublic = false;
    
    private String displayPic; // S3 key for the display picture
    
    // Media files will be handled separately in the controller
    // This is just for documentation purposes
    private List<MultipartFile> mediaFiles;
} 