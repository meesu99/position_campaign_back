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
        // 새로운 필터 구조 파싱 (enabled/value 구조)
        String gender = null;
        String sido = null;
        String sigungu = null;
        Integer ageFrom = null;
        Integer ageTo = null;
        Double centerLat = null;
        Double centerLng = null;
        Integer radiusMeters = null;
        
        // 성별 필터
        if (filters.get("gender") instanceof Map) {
            Map<String, Object> genderFilter = (Map<String, Object>) filters.get("gender");
            if (Boolean.TRUE.equals(genderFilter.get("enabled"))) {
                gender = (String) genderFilter.get("value");
            }
        }
        
        // 지역 필터
        if (filters.get("region") instanceof Map) {
            Map<String, Object> regionFilter = (Map<String, Object>) filters.get("region");
            if (Boolean.TRUE.equals(regionFilter.get("enabled")) && regionFilter.get("value") instanceof Map) {
                Map<String, Object> regionValue = (Map<String, Object>) regionFilter.get("value");
                sido = (String) regionValue.get("sido");
                sigungu = (String) regionValue.get("sigungu");
            }
        }
        
        // 나이 필터
        if (filters.get("ageRange") instanceof Map) {
            Map<String, Object> ageFilter = (Map<String, Object>) filters.get("ageRange");
            if (Boolean.TRUE.equals(ageFilter.get("enabled")) && ageFilter.get("value") instanceof List) {
                List<Integer> ageRange = (List<Integer>) ageFilter.get("value");
                if (ageRange.size() == 2) {
                    ageFrom = ageRange.get(0);
                    ageTo = ageRange.get(1);
                }
            }
        }
        
        // 반경 필터
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radiusFilter = (Map<String, Object>) filters.get("radius");
            if (Boolean.TRUE.equals(radiusFilter.get("enabled")) && radiusFilter.get("value") instanceof Map) {
                Map<String, Object> radiusValue = (Map<String, Object>) radiusFilter.get("value");
                if (radiusValue.get("lat") != null && radiusValue.get("lng") != null && radiusValue.get("meters") != null) {
                    centerLat = ((Number) radiusValue.get("lat")).doubleValue();
                    centerLng = ((Number) radiusValue.get("lng")).doubleValue();
                    radiusMeters = ((Number) radiusValue.get("meters")).intValue();
                }
            }
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
        
        // 성별 필터
        if (filters.get("gender") instanceof Map) {
            Map<String, Object> genderFilter = (Map<String, Object>) filters.get("gender");
            if (Boolean.TRUE.equals(genderFilter.get("enabled"))) {
                count++;
            }
        }
        
        // 나이 필터
        if (filters.get("ageRange") instanceof Map) {
            Map<String, Object> ageFilter = (Map<String, Object>) filters.get("ageRange");
            if (Boolean.TRUE.equals(ageFilter.get("enabled"))) {
                count++;
            }
        }
        
        // 지역 필터
        if (filters.get("region") instanceof Map) {
            Map<String, Object> regionFilter = (Map<String, Object>) filters.get("region");
            if (Boolean.TRUE.equals(regionFilter.get("enabled"))) {
                count++;
            }
        }
        
        // 반경 필터
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radiusFilter = (Map<String, Object>) filters.get("radius");
            if (Boolean.TRUE.equals(radiusFilter.get("enabled"))) {
                count++;
            }
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
        
        // 새로운 필터 구조 파싱 (previewCampaign과 동일한 로직)
        String gender = null;
        String sido = null;
        String sigungu = null;
        Integer ageFrom = null;
        Integer ageTo = null;
        Double centerLat = null;
        Double centerLng = null;
        Integer radiusMeters = null;
        
        // 성별 필터
        if (filters.get("gender") instanceof Map) {
            Map<String, Object> genderFilter = (Map<String, Object>) filters.get("gender");
            if (Boolean.TRUE.equals(genderFilter.get("enabled"))) {
                gender = (String) genderFilter.get("value");
            }
        }
        
        // 지역 필터
        if (filters.get("region") instanceof Map) {
            Map<String, Object> regionFilter = (Map<String, Object>) filters.get("region");
            if (Boolean.TRUE.equals(regionFilter.get("enabled")) && regionFilter.get("value") instanceof Map) {
                Map<String, Object> regionValue = (Map<String, Object>) regionFilter.get("value");
                sido = (String) regionValue.get("sido");
                sigungu = (String) regionValue.get("sigungu");
            }
        }
        
        // 나이 필터
        if (filters.get("ageRange") instanceof Map) {
            Map<String, Object> ageFilter = (Map<String, Object>) filters.get("ageRange");
            if (Boolean.TRUE.equals(ageFilter.get("enabled")) && ageFilter.get("value") instanceof List) {
                List<Integer> ageRange = (List<Integer>) ageFilter.get("value");
                if (ageRange.size() == 2) {
                    ageFrom = ageRange.get(0);
                    ageTo = ageRange.get(1);
                }
            }
        }
        
        // 반경 필터
        if (filters.get("radius") instanceof Map) {
            Map<String, Object> radiusFilter = (Map<String, Object>) filters.get("radius");
            if (Boolean.TRUE.equals(radiusFilter.get("enabled")) && radiusFilter.get("value") instanceof Map) {
                Map<String, Object> radiusValue = (Map<String, Object>) radiusFilter.get("value");
                if (radiusValue.get("lat") != null && radiusValue.get("lng") != null && radiusValue.get("meters") != null) {
                    centerLat = ((Number) radiusValue.get("lat")).doubleValue();
                    centerLng = ((Number) radiusValue.get("lng")).doubleValue();
                    radiusMeters = ((Number) radiusValue.get("meters")).intValue();
                }
            }
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
    
    public Map<String, Object> getCampaignStats(Long campaignId, AppUser currentUser) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다."));
        
        // 사용자 권한 확인: 본인의 캠페인이거나 ADMIN인 경우만 접근 가능
        if (!campaign.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(AppUser.Role.ADMIN)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
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

    public Map<String, Object> getDashboardStats(Long userId) {
        // 사용자의 모든 캠페인 조회
        List<Campaign> userCampaigns = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // 전체 통계 계산
        long totalSent = 0;
        long totalRead = 0;
        long totalClick = 0;
        long totalSpent = 0;
        
        for (Campaign campaign : userCampaigns) {
            if (campaign.getStatus() == Campaign.Status.COMPLETED) {
                long sent = campaignTargetRepository.countSentByCampaignId(campaign.getId());
                long read = campaignTargetRepository.countReadByCampaignId(campaign.getId());
                long click = campaignTargetRepository.countClickByCampaignId(campaign.getId());
                
                totalSent += sent;
                totalRead += read;
                totalClick += click;
                totalSpent += (campaign.getFinalCost() != null ? campaign.getFinalCost() : 0);
            }
        }
        
        // 최근 7일 캠페인 성과 데이터
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Campaign> recentCampaigns = userCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(sevenDaysAgo))
            .collect(java.util.stream.Collectors.toList());
        
        // 일별 데이터 생성 (최근 7일)
        Map<String, Object> chartData = new java.util.HashMap<>();
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Integer> sentData = new java.util.ArrayList<>();
        java.util.List<Integer> readData = new java.util.ArrayList<>();
        java.util.List<Integer> clickData = new java.util.ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.toLocalDate().toString();
            labels.add(dateStr);
            
            // 해당 날짜의 캠페인 통계 (간단한 예시)
            int dailySent = (int) recentCampaigns.stream()
                .filter(c -> c.getCreatedAt().toLocalDate().equals(date.toLocalDate()))
                .mapToInt(c -> c.getRecipientsCount() != null ? c.getRecipientsCount() : 0)
                .sum();
            
            sentData.add(dailySent);
            readData.add((int) (dailySent * 0.7)); // 예시: 70% 읽음률
            clickData.add((int) (dailySent * 0.1)); // 예시: 10% 클릭률
        }
        
        chartData.put("labels", labels);
        chartData.put("sent", sentData);
        chartData.put("read", readData);
        chartData.put("click", clickData);
        
        // 최근 캠페인 (최대 5개)
        List<Map<String, Object>> recentCampaignDtos = userCampaigns.stream()
            .limit(5)
            .map(campaign -> {
                Map<String, Object> dto = new java.util.HashMap<>();
                dto.put("id", campaign.getId());
                dto.put("title", campaign.getTitle());
                dto.put("status", campaign.getStatus().name());
                dto.put("recipientsCount", campaign.getRecipientsCount());
                dto.put("createdAt", campaign.getCreatedAt());
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return Map.of(
            "totalSent", totalSent,
            "totalRead", totalRead,
            "totalClick", totalClick,
            "totalSpent", totalSpent,
            "readRate", totalSent > 0 ? (double) totalRead / totalSent * 100 : 0,
            "clickRate", totalSent > 0 ? (double) totalClick / totalSent * 100 : 0,
            "chartData", chartData,
            "recentCampaigns", recentCampaignDtos
        );
    }
}
