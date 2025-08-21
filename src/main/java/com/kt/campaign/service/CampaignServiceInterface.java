package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.Campaign;

import java.util.List;
import java.util.Map;

/**
 * 캠페인 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface CampaignServiceInterface {
    
    /**
     * 캠페인 미리보기 - 필터 조건에 맞는 고객 수와 예상 비용 계산
     */
    Map<String, Object> previewCampaign(Map<String, Object> filters);
    
    /**
     * 새로운 캠페인 생성
     */
    Campaign createCampaign(AppUser user, String title, String messageText, String link, Map<String, Object> filters);
    
    /**
     * 캠페인 발송 실행
     */
    void sendCampaign(Long campaignId, AppUser user);
    
    /**
     * 사용자의 모든 캠페인 조회
     */
    List<Campaign> getUserCampaigns(Long userId);
    
    /**
     * 특정 캠페인의 상세 통계 조회
     */
    Map<String, Object> getCampaignStats(Long campaignId, AppUser currentUser);
    
    /**
     * 대시보드용 통계 데이터 조회
     */
    Map<String, Object> getDashboardStats(Long userId);
}
