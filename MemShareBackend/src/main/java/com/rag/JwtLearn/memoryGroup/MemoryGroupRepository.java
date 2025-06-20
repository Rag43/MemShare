package com.rag.JwtLearn.memoryGroup;

import com.rag.JwtLearn.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoryGroupRepository extends JpaRepository<MemoryGroup, Long> {
    
    Optional<MemoryGroup> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT mg FROM MemoryGroup mg JOIN mg.users u WHERE u.id = :userId")
    List<MemoryGroup> findByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT mg FROM MemoryGroup mg WHERE mg.createdBy.id = :userId")
    List<MemoryGroup> findByCreatedBy(@Param("userId") Integer userId);
    
    @Query("SELECT u FROM User u JOIN u.groups g WHERE g.id = :groupId")
    List<User> findUsersByGroupId(@Param("groupId") Long groupId);
}
