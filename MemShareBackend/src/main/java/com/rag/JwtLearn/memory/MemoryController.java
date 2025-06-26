package com.rag.JwtLearn.memory;

import com.rag.JwtLearn.memory.Memory;
import com.rag.JwtLearn.memory.dto.CreateMemoryRequest;
import com.rag.JwtLearn.memory.dto.UpdateMemoryRequest;
import com.rag.JwtLearn.memory.dto.MemoryWithGroupResponse;
import com.rag.JwtLearn.user.User;
import com.rag.JwtLearn.user.UserRepository;
import com.rag.JwtLearn.user.Role;
import com.rag.JwtLearn.config.JWTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

import java.util.List;

import com.rag.JwtLearn.memoryGroup.MemoryGroupRepository;
import com.rag.JwtLearn.memory.dto.MemoryResponse;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MemoryController {

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final MemoryService memoryService;
    private final MemoryGroupRepository memoryGroupRepository;

    /**
     * Create memory
     */
    @PostMapping
    public ResponseEntity<Memory> createMemory(
            @RequestBody CreateMemoryRequest request,
            Authentication authentication) {
        try {
            log.info("Creating memory");
            
            User currentUser = getCurrentUser(authentication);
            
            Memory memory = Memory.builder()
                    .content(request.getContent())
                    .title(request.getTitle())
                    .memoryDate(request.getMemoryDate() != null ? request.getMemoryDate() : LocalDateTime.now())
                    .location(request.getLocation())
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                    .displayPic(request.getDisplayPic())
                    .user(currentUser)
                    .build();
            
            Memory savedMemory = memoryRepository.save(memory);
            log.info("Memory created successfully: {}", savedMemory.getId());
            return ResponseEntity.ok(savedMemory);
            
        } catch (Exception e) {
            log.error("Error creating memory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{memoryId}")
    public ResponseEntity<Memory> getMemory(
            @PathVariable Long memoryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new RuntimeException("Memory not found"));
        
        // Check if user owns the memory or if it's public
        if (!memory.getUser().getId().equals(user.getId()) && !memory.getIsPublic()) {
            throw new RuntimeException("Unauthorized access to memory");
        }
        
        return ResponseEntity.ok(memory);
    }

    @GetMapping("/my-memories")
    public ResponseEntity<List<MemoryWithGroupResponse>> getMyMemories(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<MemoryWithGroupResponse> memories = memoryService.getAllMemoriesForUser(user);
        return ResponseEntity.ok(memories);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<MemoryResponse>> getMemoriesByGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        List<Memory> memories = memoryService.getMemoriesByGroup(groupId, currentUser);
        
        // Convert to MemoryResponse DTOs
        List<MemoryResponse> memoryResponses = memories.stream()
                .map(memory -> MemoryResponse.builder()
                        .id(memory.getId())
                        .title(memory.getTitle())
                        .content(memory.getContent())
                        .memoryDate(memory.getMemoryDate())
                        .location(memory.getLocation())
                        .isPublic(memory.getIsPublic())
                        .displayPic(memory.getDisplayPic())
                        .userId(memory.getUser().getId())
                        .createdAt(memory.getCreatedAt())
                        .updatedAt(memory.getUpdatedAt())
                        .media(memory.getMedia())
                        .build())
                .toList();
        
        return ResponseEntity.ok(memoryResponses);
    }

    @PutMapping("/{memoryId}")
    public ResponseEntity<Memory> updateMemory(
            @PathVariable Long memoryId,
            @RequestBody UpdateMemoryRequest request,
            Authentication authentication) {
        
        try {
            log.info("Updating memory with ID: {}", memoryId);
            
            User currentUser = getCurrentUser(authentication);
            Memory memory = memoryRepository.findById(memoryId)
                    .orElseThrow(() -> new RuntimeException("Memory not found"));
            
            // Check if user owns the memory
            if (!memory.getUser().getId().equals(currentUser.getId())) {
                log.warn("Unauthorized update attempt for memory {} by user {}", memoryId, currentUser.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Update fields if provided
            if (request.getTitle() != null) {
                memory.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                memory.setContent(request.getContent());
            }
            if (request.getMemoryDate() != null) {
                memory.setMemoryDate(request.getMemoryDate());
            }
            if (request.getLocation() != null) {
                memory.setLocation(request.getLocation());
            }
            if (request.getDisplayPic() != null) {
                memory.setDisplayPic(request.getDisplayPic());
            }
            
            Memory updatedMemory = memoryRepository.save(memory);
            log.info("Memory updated successfully: {}", memoryId);
            return ResponseEntity.ok(updatedMemory);
            
        } catch (RuntimeException e) {
            log.error("Error updating memory {}: {}", memoryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error updating memory {}: {}", memoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete memory
     */
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> deleteMemory(
            @PathVariable Long memoryId,
            Authentication authentication) {
        
        try {
            log.info("Deleting memory with ID: {}", memoryId);
            
            User user = getCurrentUser(authentication);
            Memory memory = memoryRepository.findById(memoryId)
                    .orElseThrow(() -> new RuntimeException("Memory not found"));
            
            // Check if user owns the memory or if user is admin
            if (!memory.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
                log.warn("Unauthorized delete attempt for memory {} by user {}", memoryId, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            memoryRepository.delete(memory);
            log.info("Memory deleted successfully: {}", memoryId);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            log.error("Error deleting memory {}: {}", memoryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error deleting memory {}: {}", memoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create memory without authentication (for testing)
     */
    @PostMapping("/test-create")
    public ResponseEntity<Memory> createMemoryTest(@RequestBody CreateMemoryRequest request) {
        try {
            log.info("Creating memory without authentication for testing");
            
            // Use a default user (ID 1) for testing
            User defaultUser = userRepository.findById(1)
                    .orElseThrow(() -> new RuntimeException("Default user not found"));
            
            Memory memory = Memory.builder()
                    .content(request.getContent())
                    .title(request.getTitle())
                    .memoryDate(request.getMemoryDate() != null ? request.getMemoryDate() : LocalDateTime.now())
                    .location(request.getLocation())
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                    .user(defaultUser)
                    .build();
            
            Memory savedMemory = memoryRepository.save(memory);
            log.info("Memory created successfully: {}", savedMemory.getId());
            return ResponseEntity.ok(savedMemory);
            
        } catch (Exception e) {
            log.error("Error creating memory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User getCurrentUser(Authentication authentication) {
        // Use email to find user (this was the original working approach)
        String email = authentication.getName();
        log.debug("Using email to find user: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
