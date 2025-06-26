package com.rag.JwtLearn.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemoryRequest {
    private String title;
    private String content;
    private LocalDateTime memoryDate;
    private String location;
    private String displayPic;
} 