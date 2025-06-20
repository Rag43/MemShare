package com.rag.JwtLearn.memoryGroup;

import com.rag.JwtLearn.memoryGroup.dto.CreateGroupRequest;
import com.rag.JwtLearn.memoryGroup.dto.GroupResponse;
import com.rag.JwtLearn.user.User;
import com.rag.JwtLearn.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MemoryGroupServiceTest {

    @Autowired
    private MemoryGroupService memoryGroupService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateGroup() {
        // Create a test user
        User testUser = User.builder()
                .firstname("Test")
                .lastname("User")
                .email("test@example.com")
                .password("password")
                .role(com.rag.JwtLearn.user.Role.USER)
                .build();
        
        User savedUser = userRepository.save(testUser);

        // Create group request
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Test Group")
                .description("A test group")
                .userIds(new HashSet<>())
                .build();

        // Create group
        GroupResponse groupResponse = memoryGroupService.createGroup(request, savedUser);

        // Verify group was created
        assertNotNull(groupResponse);
        assertEquals("Test Group", groupResponse.getName());
        assertEquals("A test group", groupResponse.getDescription());
        assertEquals("test@example.com", groupResponse.getCreatedBy());
        assertEquals(1, groupResponse.getUsers().size());
        assertTrue(groupResponse.getUsers().stream()
                .anyMatch(user -> user.getEmail().equals("test@example.com")));
    }

    @Test
    public void testCreateGroupWithMultipleUsers() {
        // Create test users
        User user1 = userRepository.save(User.builder()
                .firstname("User1")
                .lastname("Test")
                .email("user1@example.com")
                .password("password")
                .role(com.rag.JwtLearn.user.Role.USER)
                .build());

        User user2 = userRepository.save(User.builder()
                .firstname("User2")
                .lastname("Test")
                .email("user2@example.com")
                .password("password")
                .role(com.rag.JwtLearn.user.Role.USER)
                .build());

        // Create group request with multiple users
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Multi User Group")
                .description("A group with multiple users")
                .userIds(new HashSet<>() {{
                    add(user2.getId());
                }})
                .build();

        // Create group
        GroupResponse groupResponse = memoryGroupService.createGroup(request, user1);

        // Verify group was created with both users
        assertNotNull(groupResponse);
        assertEquals("Multi User Group", groupResponse.getName());
        assertEquals(2, groupResponse.getUsers().size());
        assertTrue(groupResponse.getUsers().stream()
                .anyMatch(user -> user.getEmail().equals("user1@example.com")));
        assertTrue(groupResponse.getUsers().stream()
                .anyMatch(user -> user.getEmail().equals("user2@example.com")));
    }
} 