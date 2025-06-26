package com.rag.JwtLearn.memory;

import com.rag.JwtLearn.memoryGroup.MemoryGroup;
import com.rag.JwtLearn.memoryGroup.MemoryGroupRepository;
import com.rag.JwtLearn.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.rag.JwtLearn.memory.dto.MemoryWithGroupResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryGroupRepository memoryGroupRepository;
    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    public List<Memory> getMemoriesByGroup(Long groupId, User currentUser) {
        // Get the memory group
        MemoryGroup group = memoryGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Memory group not found"));

        // Check if current user is a member of the group
        if (!group.getUsers().contains(currentUser)) {
            throw new RuntimeException("User is not a member of this group");
        }

        // Get all user IDs in the group
        List<Integer> userIds = group.getUsers().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Get memories from all users in the group with media
        return memoryRepository.findByUserIdInWithMedia(userIds);
    }

    public List<MemoryWithGroupResponse> getAllMemoriesForUser(User currentUser) {
        List<MemoryWithGroupResponse> allMemories = new ArrayList<>();
        
        // Get all groups the user is a member of
        List<MemoryGroup> userGroups = memoryGroupRepository.findByUserId(currentUser.getId());
        log.info("User {} is in {} groups", currentUser.getId(), userGroups.size());
        
        for (MemoryGroup group : userGroups) {
            log.info("Processing group: {} (ID: {})", group.getName(), group.getId());
            
            // Get all user IDs in this group
            List<Integer> userIds = group.getUsers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            
            // Get memories from all users in this group with media
            List<Memory> groupMemories = memoryRepository.findByUserIdInWithMedia(userIds);
            log.info("Found {} memories in group {}", groupMemories.size(), group.getName());
            
            // Convert to DTOs with group name
            for (Memory memory : groupMemories) {
                log.info("Processing memory: {} (ID: {}), media count: {}", 
                        memory.getTitle(), memory.getId(), memory.getMedia().size());
                MemoryWithGroupResponse response = MemoryWithGroupResponse.fromMemory(memory, group.getName());
                allMemories.add(response);
            }
        }
        
        // Sort by memory date (newest first)
        allMemories.sort((m1, m2) -> m2.getMemoryDate().compareTo(m1.getMemoryDate()));
        
        log.info("Returning {} total memories for user {}", allMemories.size(), currentUser.getId());
        return allMemories;
    }
}
