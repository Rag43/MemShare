package com.rag.JwtLearn.memoryGroup;

import com.rag.JwtLearn.memoryGroup.dto.AddUsersRequest;
import com.rag.JwtLearn.memoryGroup.dto.CreateGroupRequest;
import com.rag.JwtLearn.memoryGroup.dto.GroupResponse;
import com.rag.JwtLearn.user.User;
import com.rag.JwtLearn.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemoryGroupController {

    private final MemoryGroupService memoryGroupService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody CreateGroupRequest request) {
        User currentUser = getCurrentUser();
        GroupResponse group = memoryGroupService.createGroup(request, currentUser);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long groupId) {
        GroupResponse group = memoryGroupService.getGroupById(groupId);
        return ResponseEntity.ok(group);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        List<GroupResponse> groups = memoryGroupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponse>> getGroupsByUserId(@PathVariable Integer userId) {
        List<GroupResponse> groups = memoryGroupService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<List<GroupResponse>> getGroupsCreatedByUser(@PathVariable Integer userId) {
        List<GroupResponse> groups = memoryGroupService.getGroupsCreatedByUser(userId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupResponse>> getMyGroups() {
        User currentUser = getCurrentUser();
        List<GroupResponse> groups = memoryGroupService.getGroupsByUserId(currentUser.getId());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/my-created-groups")
    public ResponseEntity<List<GroupResponse>> getMyCreatedGroups() {
        User currentUser = getCurrentUser();
        List<GroupResponse> groups = memoryGroupService.getGroupsCreatedByUser(currentUser.getId());
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/{groupId}/users")
    public ResponseEntity<GroupResponse> addUsersToGroup(
            @PathVariable Long groupId,
            @RequestBody AddUsersRequest request) {
        GroupResponse group = memoryGroupService.addUsersToGroup(groupId, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}/users")
    public ResponseEntity<GroupResponse> removeUsersFromGroup(
            @PathVariable Long groupId,
            @RequestBody AddUsersRequest request) {
        GroupResponse group = memoryGroupService.removeUsersFromGroup(groupId, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        memoryGroupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
