package com.rag.JwtLearn.memory;

import com.rag.JwtLearn.memory.Memory;
import com.rag.JwtLearn.memory.dto.CreateMemoryRequest;
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

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
@Slf4j
public class MemoryController {

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;
    private final JWTService jwtService;

    /**
     * Create memory
     */
    @PostMapping
    public ResponseEntity<Memory> createMemory(@RequestBody CreateMemoryRequest request) {
        try {
            log.info("Creating memory");
            
            // Find or create a default user
            User defaultUser = userRepository.findById(1)
                    .orElseGet(() -> {
                        log.info("Default user not found, creating one");
                        User newUser = User.builder()
                                .firstname("Default")
                                .lastname("User")
                                .email("default@example.com")
                                .password("$2a$10$dummy") // Dummy password
                                .role(Role.USER)
                                .build();
                        return userRepository.save(newUser);
                    });
            
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
    public ResponseEntity<List<Memory>> getMyMemories(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Memory> memories = memoryRepository.findByUserId(user.getId());
        return ResponseEntity.ok(memories);
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
