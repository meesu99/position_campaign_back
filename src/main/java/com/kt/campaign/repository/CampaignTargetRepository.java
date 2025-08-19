package com.kt.campaign.repository;

import com.kt.campaign.entity.CampaignTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignTargetRepository extends JpaRepository<CampaignTarget, Long> {
    
    List<CampaignTarget> findByCampaignId(Long campaignId);
    
    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.campaign.id = :campaignId AND ct.sentAt IS NOT NULL")
    long countSentByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.campaign.id = :campaignId AND ct.readAt IS NOT NULL")
    long countReadByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(ct) FROM CampaignTarget ct WHERE ct.campaign.id = :campaignId AND ct.clickAt IS NOT NULL")
    long countClickByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT ct FROM CampaignTarget ct WHERE ct.campaign.user.id = :userId AND ct.sentAt >= :startTime")
    List<CampaignTarget> findByUserIdAndSentAtAfter(@Param("userId") Long userId,
                                                   @Param("startTime") LocalDateTime startTime);
}
