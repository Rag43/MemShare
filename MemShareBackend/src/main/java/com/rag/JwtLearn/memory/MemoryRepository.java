package com.rag.JwtLearn.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {
    
    List<Memory> findByUserId(Integer userId);
    
    List<Memory> findByUserIdIn(List<Integer> userIds);
    
    @Query("SELECT DISTINCT m FROM Memory m LEFT JOIN FETCH m.media WHERE m.user.id IN :userIds ORDER BY m.memoryDate DESC")
    List<Memory> findByUserIdInWithMedia(@Param("userIds") List<Integer> userIds);
}
