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
    
    @Query("SELECT ct FROM CampaignTarget ct LEFT JOIN FETCH ct.campaign c LEFT JOIN FETCH c.user " +
           "WHERE ct.customer.id = :customerId ORDER BY ct.sentAt DESC")
    List<CampaignTarget> findByCustomerIdOrderBySentAtDesc(@Param("customerId") Long customerId);
    
    @Query(value = "SELECT " +
                   "CASE " +
                   "WHEN (2024 - c.birth_year) < 30 THEN '20대' " +
                   "WHEN (2024 - c.birth_year) < 40 THEN '30대' " +
                   "WHEN (2024 - c.birth_year) < 50 THEN '40대' " +
                   "WHEN (2024 - c.birth_year) < 60 THEN '50대' " +
                   "ELSE '60대' " +
                   "END as age_group, " +
                   "CASE " +
                   "WHEN c.gender = 'M' THEN '남성' " +
                   "WHEN c.gender = 'F' THEN '여성' " +
                   "ELSE c.gender " +
                   "END as gender, " +
                   "COUNT(*) as count " +
                   "FROM campaign_targets ct " +
                   "JOIN customers c ON ct.customer_id = c.id " +
                   "JOIN campaigns camp ON ct.campaign_id = camp.id " +
                   "WHERE camp.user_id = :userId AND camp.status = 'COMPLETED' " +
                   "GROUP BY age_group, gender " +
                   "ORDER BY age_group, gender", 
           nativeQuery = true)
    List<Object[]> getAgeGenderDistributionByUserId(@Param("userId") Long userId);
    
    // 시간별 통계를 위한 메서드들
    @Query("SELECT HOUR(ct.sentAt) as hour, COUNT(ct) as count FROM CampaignTarget ct " +
           "WHERE ct.campaign.id = :campaignId AND ct.sentAt IS NOT NULL " +
           "GROUP BY HOUR(ct.sentAt) ORDER BY hour")
    List<Object[]> findHourlySentStatsByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT HOUR(ct.readAt) as hour, COUNT(ct) as count FROM CampaignTarget ct " +
           "WHERE ct.campaign.id = :campaignId AND ct.readAt IS NOT NULL " +
           "GROUP BY HOUR(ct.readAt) ORDER BY hour")
    List<Object[]> findHourlyReadStatsByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT HOUR(ct.clickAt) as hour, COUNT(ct) as count FROM CampaignTarget ct " +
           "WHERE ct.campaign.id = :campaignId AND ct.clickAt IS NOT NULL " +
           "GROUP BY HOUR(ct.clickAt) ORDER BY hour")
    List<Object[]> findHourlyClickStatsByCampaignId(@Param("campaignId") Long campaignId);
}
