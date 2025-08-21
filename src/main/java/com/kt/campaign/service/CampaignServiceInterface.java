package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.Campaign;

import java.util.List;
import java.util.Map;

/**
 * 캠페인 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * 이 인터페이스는 캠페인의 생성, 발송, 통계 조회 등의 핵심 비즈니스 로직을 정의합니다.
 * 구현체는 CampaignService 클래스이며, 의존성 역전 원칙을 적용하여
 * 컨트롤러가 구체적인 구현이 아닌 인터페이스에 의존하도록 설계되었습니다.
 * 
 * @author KT 위치 문자 서비스 팀
 * @version 1.0
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
