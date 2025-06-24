package com.rag.JwtLearn.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String content;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private LocalDateTime memoryDate = LocalDateTime.now(); // Default to current time
    
    private String location;
    
    private Boolean isPublic = false;
    
    // Media files will be handled separately in the controller
    // This is just for documentation purposes
    private List<MultipartFile> mediaFiles;
} 