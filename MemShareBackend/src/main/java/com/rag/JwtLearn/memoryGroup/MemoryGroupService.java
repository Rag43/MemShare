package com.rag.JwtLearn.memoryGroup;

import com.rag.JwtLearn.memoryGroup.dto.AddUsersRequest;
import com.rag.JwtLearn.memoryGroup.dto.CreateGroupRequest;
import com.rag.JwtLearn.memoryGroup.dto.GroupResponse;
import com.rag.JwtLearn.user.User;
import com.rag.JwtLearn.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoryGroupService {

    private final MemoryGroupRepository memoryGroupRepository;
    private final UserRepository userRepository;

    public GroupResponse createGroup(CreateGroupRequest request, User currentUser) {
        // Check if group name already exists
        if (memoryGroupRepository.existsByName(request.getName())) {
            throw new RuntimeException("Group with name '" + request.getName() + "' already exists");
        }

        // Create new group
        MemoryGroup group = MemoryGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(currentUser)
                .build();

        // Add current user to the group
        group.addUser(currentUser);

        // Add other users if specified
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            Set<User> users = userRepository.findAllById(request.getUserIds())
                    .stream()
                    .collect(Collectors.toSet());
            
            for (User user : users) {
                group.addUser(user);
            }
        }

        MemoryGroup savedGroup = memoryGroupRepository.save(group);
        return convertToGroupResponse(savedGroup);
    }

    public GroupResponse getGroupById(Long groupId) {
        MemoryGroup group = memoryGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
        return convertToGroupResponse(group);
    }

    public List<GroupResponse> getAllGroups() {
        return memoryGroupRepository.findAll()
                .stream()
                .map(this::convertToGroupResponse)
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getGroupsByUserId(Integer userId) {
        return memoryGroupRepository.findByUserId(userId)
                .stream()
                .map(this::convertToGroupResponse)
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getGroupsCreatedByUser(Integer userId) {
        return memoryGroupRepository.findByCreatedBy(userId)
                .stream()
                .map(this::convertToGroupResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse addUsersToGroup(Long groupId, AddUsersRequest request) {
        MemoryGroup group = memoryGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        Set<User> users = userRepository.findAllById(request.getUserIds())
                .stream()
                .collect(Collectors.toSet());

        for (User user : users) {
            group.addUser(user);
        }

        MemoryGroup savedGroup = memoryGroupRepository.save(group);
        return convertToGroupResponse(savedGroup);
    }

    public GroupResponse removeUsersFromGroup(Long groupId, AddUsersRequest request) {
        MemoryGroup group = memoryGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        Set<User> users = userRepository.findAllById(request.getUserIds())
                .stream()
                .collect(Collectors.toSet());

        for (User user : users) {
            group.removeUser(user);
        }

        MemoryGroup savedGroup = memoryGroupRepository.save(group);
        return convertToGroupResponse(savedGroup);
    }

    public void deleteGroup(Long groupId) {
        if (!memoryGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Group not found with id: " + groupId);
        }
        memoryGroupRepository.deleteById(groupId);
    }

    private GroupResponse convertToGroupResponse(MemoryGroup group) {
        Set<GroupResponse.UserSummary> userSummaries = group.getUsers()
                .stream()
                .map(user -> GroupResponse.UserSummary.builder()
                        .id(user.getId())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toSet());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy().getEmail())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .users(userSummaries)
                .build();
    }
}
