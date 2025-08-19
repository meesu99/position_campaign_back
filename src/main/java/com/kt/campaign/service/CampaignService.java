package com.kt.campaign.service;

import com.kt.campaign.entity.*;
import com.kt.campaign.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    
    private final CampaignRepository campaignRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final CustomerRepository customerRepository;
    private final WalletService walletService;
    
    public Map<String, Object> previewCampaign(Map<String, Object> filters) {
        // 필터 파싱
        String gender = (String) filters.get("gender");
        String sido = null;
        String sigungu = null;
        Integer ageFrom = null;
        Integer ageTo = null;
        Double centerLat = null;
        Double centerLng = null;
        Integer radiusMeters = null;
        
        if (filters.get("region") instanceof Map) {
            Map<String, Object> region = (Map<String, Object>) filters.get("region");
            sido = (String) region.get("sido");
            sigungu = (String) region.get("sigungu");
        }
        
        if (filters.get("ageRange") instanceof List) {
            List<Integer> ageRange = (List<Integer>) filters.get("ageRange");
            if (ageRange.size() == 2) {
                ageFrom = ageRange.get(0);
                ageTo = ageRange.get(1);
            }
        }
        
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radius = (Map<String, Object>) filters.get("radius");
            centerLat = ((Number) radius.get("lat")).doubleValue();
            centerLng = ((Number) radius.get("lng")).doubleValue();
            radiusMeters = ((Number) radius.get("meters")).intValue();
        }
        
        // 수신자 수 계산
        long recipients = customerRepository.countByFiltersWithRadius(
            gender, sido, sigungu, ageFrom, ageTo, centerLat, centerLng, radiusMeters
        );
        
        // 단가 계산
        int activeFilters = countActiveFilters(filters);
        int unitPrice = calculateUnitPrice(activeFilters);
        long estimatedCost = recipients * unitPrice;
        
        return Map.of(
            "recipients", recipients,
            "unitPrice", unitPrice,
            "estimatedCost", estimatedCost
        );
    }
    
    private int countActiveFilters(Map<String, Object> filters) {
        int count = 0;
        
        if (filters.get("gender") != null && !((String) filters.get("gender")).isEmpty()) {
            count++;
        }
        
        if (filters.get("ageRange") instanceof List) {
            List<Integer> ageRange = (List<Integer>) filters.get("ageRange");
            if (ageRange.size() == 2) count++;
        }
        
        if (filters.get("region") instanceof Map) {
            Map<String, Object> region = (Map<String, Object>) filters.get("region");
            String sido = (String) region.get("sido");
            String sigungu = (String) region.get("sigungu");
            if ((sido != null && !sido.isEmpty()) || (sigungu != null && !sigungu.isEmpty())) {
                count++;
            }
        }
        
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radius = (Map<String, Object>) filters.get("radius");
            Integer meters = ((Number) radius.get("meters")).intValue();
            if (meters > 0) count++;
        }
        
        return count;
    }
    
    private int calculateUnitPrice(int activeFilters) {
        int[] prices = {0, 50, 70, 90, 110, 130};
        return prices[Math.min(activeFilters, 5)];
    }
    
    @Transactional
    public Campaign createCampaign(AppUser user, String title, String messageText, String link, Map<String, Object> filters) {
        Map<String, Object> preview = previewCampaign(filters);
        
        Campaign campaign = new Campaign();
        campaign.setUser(user);
        campaign.setTitle(title);
        campaign.setMessageText(messageText);
        campaign.setLink(link);
        campaign.setFilters(filters);
        campaign.setPricePerRecipient((Integer) preview.get("unitPrice"));
        campaign.setEstimatedCost((Long) preview.get("estimatedCost"));
        campaign.setRecipientsCount(((Long) preview.get("recipients")).intValue());
        campaign.setStatus(Campaign.Status.DRAFT);
        
        return campaignRepository.save(campaign);
    }
    
    @Transactional
    public void sendCampaign(Long campaignId, AppUser user) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다."));
        
        if (!campaign.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        if (campaign.getStatus() != Campaign.Status.DRAFT) {
            throw new IllegalArgumentException("발송할 수 없는 캠페인 상태입니다.");
        }
        
        // 포인트 확인 및 차감
        walletService.debitForCampaign(user.getId(), campaign.getEstimatedCost(), campaign.getId());
        
        // 타겟 생성
        createCampaignTargets(campaign);
        
        // 상태 업데이트
        campaign.setStatus(Campaign.Status.SENDING);
        campaign.setFinalCost(campaign.getEstimatedCost());
        campaignRepository.save(campaign);
        
        // 비동기 발송 처리 (실제로는 워커 큐에 추가)
        processCampaignAsync(campaign);
    }
    
    private void createCampaignTargets(Campaign campaign) {
        Map<String, Object> filters = campaign.getFilters();
        
        // 필터 파싱 (previewCampaign과 동일한 로직)
        String gender = (String) filters.get("gender");
        String sido = null;
        String sigungu = null;
        Integer ageFrom = null;
        Integer ageTo = null;
        Double centerLat = null;
        Double centerLng = null;
        Integer radiusMeters = null;
        
        if (filters.get("region") instanceof Map) {
            Map<String, Object> region = (Map<String, Object>) filters.get("region");
            sido = (String) region.get("sido");
            sigungu = (String) region.get("sigungu");
        }
        
        if (filters.get("ageRange") instanceof List) {
            List<Integer> ageRange = (List<Integer>) filters.get("ageRange");
            if (ageRange.size() == 2) {
                ageFrom = ageRange.get(0);
                ageTo = ageRange.get(1);
            }
        }
        
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radius = (Map<String, Object>) filters.get("radius");
            centerLat = ((Number) radius.get("lat")).doubleValue();
            centerLng = ((Number) radius.get("lng")).doubleValue();
            radiusMeters = ((Number) radius.get("meters")).intValue();
        }
        
        List<Customer> customers = customerRepository.findByFiltersWithRadius(
            gender, sido, sigungu, ageFrom, ageTo, centerLat, centerLng, radiusMeters
        );
        
        for (Customer customer : customers) {
            CampaignTarget target = new CampaignTarget();
            target.setCampaign(campaign);
            target.setCustomer(customer);
            target.setDeliveryStatus(CampaignTarget.DeliveryStatus.PENDING);
            campaignTargetRepository.save(target);
        }
    }
    
    private void processCampaignAsync(Campaign campaign) {
        // 실제로는 별도 스레드나 메시지 큐에서 처리
        // 여기서는 즉시 완료 처리
        List<CampaignTarget> targets = campaignTargetRepository.findByCampaignId(campaign.getId());
        
        for (CampaignTarget target : targets) {
            target.setDeliveryStatus(CampaignTarget.DeliveryStatus.DELIVERED);
            target.setSentAt(LocalDateTime.now());
            campaignTargetRepository.save(target);
        }
        
        campaign.setStatus(Campaign.Status.COMPLETED);
        campaignRepository.save(campaign);
    }
    
    public List<Campaign> getUserCampaigns(Long userId) {
        return campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Map<String, Object> getCampaignStats(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다."));
        
        long sent = campaignTargetRepository.countSentByCampaignId(campaignId);
        long read = campaignTargetRepository.countReadByCampaignId(campaignId);
        long click = campaignTargetRepository.countClickByCampaignId(campaignId);
        
        return Map.of(
            "campaign", campaign,
            "sent", sent,
            "read", read,
            "click", click,
            "readRate", sent > 0 ? (double) read / sent * 100 : 0,
            "clickRate", sent > 0 ? (double) click / sent * 100 : 0
        );
    }
}
