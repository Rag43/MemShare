package com.rag.JwtLearn.memory.dto;

import com.rag.JwtLearn.memory.Memory;
import com.rag.JwtLearn.media.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MemoryWithGroupResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime memoryDate;
    private String location;
    private String displayPic;
    private Boolean isPublic;
    private String groupName;
    private List<Media> media;
    
    public static MemoryWithGroupResponse fromMemory(Memory memory, String groupName) {
        return MemoryWithGroupResponse.builder()
                .id(memory.getId())
                .title(memory.getTitle())
                .content(memory.getContent())
                .memoryDate(memory.getMemoryDate())
                .location(memory.getLocation())
                .displayPic(memory.getDisplayPic())
                .isPublic(memory.getIsPublic())
                .groupName(groupName)
                .media(memory.getMedia())
                .build();
    }
} 