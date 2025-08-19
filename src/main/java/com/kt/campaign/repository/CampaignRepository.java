package com.kt.campaign.repository;

import com.kt.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    List<Campaign> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT c FROM Campaign c WHERE c.user.id = :userId AND c.createdAt >= :startTime")
    List<Campaign> findByUserIdAndCreatedAtAfter(@Param("userId") Long userId, 
                                                @Param("startTime") LocalDateTime startTime);
}
