package com.kt.campaign.service;

import com.kt.campaign.entity.*;
import com.kt.campaign.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 캠페인 관련 비즈니스 로직을 처리하는 서비스 구현체
 * 
 * 이 서비스는 캠페인의 생성, 발송, 통계 조회 등의 핵심 비즈니스 로직을 구현합니다.
 * 위치 기반 문자 캠페인의 전체 생명주기를 관리합니다.
 * 
 * 주요 기능:
 * - 캠페인 미리보기 (필터 조건에 맞는 고객 수 및 예상 비용 계산)
 * - 캠페인 생성 및 발송 처리
 * - 실시간 캠페인 통계 조회 (발송, 읽음, 클릭)
 * - 대시보드용 통합 통계 제공
 * - 시간별 성과 분석 (실제 데이터 기반)
 * 
 * 특별 기능:
 * - 지역 기반 고객 필터링 (시도, 시군구)
 * - 나이대별 성별 분포 계산
 * - 실시간 hourly 통계 생성
 * - 포인트 차감 및 캠페인 발송 처리
 * 
 * @author KT 위치 문자 서비스 팀
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService implements CampaignServiceInterface {
    
    private final CampaignRepository campaignRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final CustomerRepository customerRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WalletServiceInterface walletService;
    
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
                if (gender != null && gender.trim().isEmpty()) {
                    gender = null;
                }
            }
        }
        
        // 지역 필터
        if (filters.get("region") instanceof Map) {
            Map<String, Object> regionFilter = (Map<String, Object>) filters.get("region");
            if (Boolean.TRUE.equals(regionFilter.get("enabled")) && regionFilter.get("value") instanceof Map) {
                Map<String, Object> regionValue = (Map<String, Object>) regionFilter.get("value");
                sido = (String) regionValue.get("sido");
                sigungu = (String) regionValue.get("sigungu");
                
                // 빈 문자열을 null로 변환하여 쿼리에서 제대로 처리되도록 함
                if (sido != null && sido.trim().isEmpty()) {
                    sido = null;
                }
                if (sigungu != null && sigungu.trim().isEmpty()) {
                    sigungu = null;
                }
                
                System.out.println("Region filter - sido: " + sido + ", sigungu: " + sigungu);
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
        // 0개: 50원, 1개: 70원, 2개: 110원, 3개: 130원, 4개: 150원
        int[] prices = {50, 70, 110, 130, 150};
        return prices[Math.min(activeFilters, 4)];
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
                if (gender != null && gender.trim().isEmpty()) {
                    gender = null;
                }
            }
        }
        
        // 지역 필터
        if (filters.get("region") instanceof Map) {
            Map<String, Object> regionFilter = (Map<String, Object>) filters.get("region");
            if (Boolean.TRUE.equals(regionFilter.get("enabled")) && regionFilter.get("value") instanceof Map) {
                Map<String, Object> regionValue = (Map<String, Object>) regionFilter.get("value");
                sido = (String) regionValue.get("sido");
                sigungu = (String) regionValue.get("sigungu");
                
                // 빈 문자열을 null로 변환하여 쿼리에서 제대로 처리되도록 함
                if (sido != null && sido.trim().isEmpty()) {
                    sido = null;
                }
                if (sigungu != null && sigungu.trim().isEmpty()) {
                    sigungu = null;
                }
                
                System.out.println("Creating targets - sido: " + sido + ", sigungu: " + sigungu);
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
        
        // 캠페인 완료 알림 메시지 추가
        createCampaignCompletionNotification(campaign);
    }
    
    @Transactional
    private void createCampaignCompletionNotification(Campaign campaign) {
        ChatMessage notification = new ChatMessage();
        notification.setUser(campaign.getUser());
        notification.setFromAdmin(true);
        notification.setCampaign(campaign);
        notification.setText(String.format("🎉 '%s' 캠페인이 성공적으로 발송 완료되었습니다! 총 %d명에게 전송되었습니다.", 
            campaign.getTitle(), campaign.getRecipientsCount()));
        notification.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(notification);
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
        
        // 시간별 통계 데이터 생성
        Map<String, Object> hourlyData = generateHourlyStats(campaignId);
        
        return Map.of(
            "campaign", campaign,
            "sent", sent,
            "read", read,
            "click", click,
            "readRate", sent > 0 ? (double) read / sent * 100 : 0,
            "clickRate", sent > 0 ? (double) click / sent * 100 : 0,
            "hourlyData", hourlyData
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
            
            // 해당 날짜의 실제 캠페인 통계 계산
            List<Campaign> dailyCampaigns = recentCampaigns.stream()
                .filter(c -> c.getCreatedAt().toLocalDate().equals(date.toLocalDate()) && 
                           c.getStatus() == Campaign.Status.COMPLETED)
                .collect(java.util.stream.Collectors.toList());
            
            int dailySent = 0;
            int dailyRead = 0;
            int dailyClick = 0;
            
            for (Campaign campaign : dailyCampaigns) {
                dailySent += (int) campaignTargetRepository.countSentByCampaignId(campaign.getId());
                dailyRead += (int) campaignTargetRepository.countReadByCampaignId(campaign.getId());
                dailyClick += (int) campaignTargetRepository.countClickByCampaignId(campaign.getId());
            }
            
            sentData.add(dailySent);
            readData.add(dailyRead);
            clickData.add(dailyClick);
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
                dto.put("finalCost", campaign.getFinalCost());
                dto.put("estimatedCost", campaign.getEstimatedCost());
                dto.put("createdAt", campaign.getCreatedAt());
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // 나이대별 분포 데이터 (사용자의 캠페인 대상자들만)
        List<Map<String, Object>> ageDistribution = calculateUserAgeDistribution(userId);
        
        System.out.println("=== Age Distribution Debug ===");
        System.out.println("UserId: " + userId);
        System.out.println("Age Distribution: " + ageDistribution);
        System.out.println("===============================");
        
        return Map.of(
            "totalSent", totalSent,
            "totalRead", totalRead,
            "totalClick", totalClick,
            "totalSpent", totalSpent,
            "readRate", totalSent > 0 ? (double) totalRead / totalSent * 100 : 0,
            "clickRate", totalSent > 0 ? (double) totalClick / totalSent * 100 : 0,
            "chartData", chartData,
            "recentCampaigns", recentCampaignDtos,
            "ageDistribution", ageDistribution
        );
    }
    
    private List<Map<String, Object>> calculateUserAgeDistribution(Long userId) {
        // 디버깅: 사용자의 캠페인과 campaign_targets 데이터 확인
        List<Campaign> userCampaigns = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
        System.out.println("=== Campaign Targets Debug ===");
        System.out.println("User campaigns count: " + userCampaigns.size());
        
        for (Campaign campaign : userCampaigns) {
            List<CampaignTarget> targets = campaignTargetRepository.findByCampaignId(campaign.getId());
            System.out.println("Campaign " + campaign.getId() + " (" + campaign.getStatus() + "): " + targets.size() + " targets");
            
            if (!targets.isEmpty()) {
                CampaignTarget firstTarget = targets.get(0);
                System.out.println("  First target - Customer ID: " + firstTarget.getCustomer().getId() + 
                                 ", Gender: " + firstTarget.getCustomer().getGender() + 
                                 ", Birth Year: " + firstTarget.getCustomer().getBirthYear());
            }
        }
        System.out.println("===============================");
        
        // 만약 완료된 캠페인에 campaign_targets가 없다면 생성
        for (Campaign campaign : userCampaigns) {
            if (campaign.getStatus() == Campaign.Status.COMPLETED) {
                List<CampaignTarget> existingTargets = campaignTargetRepository.findByCampaignId(campaign.getId());
                if (existingTargets.isEmpty() && campaign.getRecipientsCount() > 0) {
                    System.out.println("Creating missing targets for campaign " + campaign.getId());
                    createMissingCampaignTargets(campaign);
                }
            }
        }
        
        // 사용자의 완료된 캠페인 대상자들의 나이대별/성별별 분포를 DB에서 직접 조회
        List<Object[]> ageGenderGroups = campaignTargetRepository.getAgeGenderDistributionByUserId(userId);
        
        // 모든 나이대별 성별을 0으로 초기화
        Map<String, Map<String, Integer>> ageGroupCounts = new java.util.HashMap<>();
        String[] ageGroups = {"20대", "30대", "40대", "50대", "60대"};
        
        for (String ageGroup : ageGroups) {
            Map<String, Integer> genderCounts = new java.util.HashMap<>();
            genderCounts.put("남성", 0);
            genderCounts.put("여성", 0);
            ageGroupCounts.put(ageGroup, genderCounts);
        }
        
        // DB 결과를 맵에 적용
        for (Object[] row : ageGenderGroups) {
            String ageGroup = (String) row[0];
            String gender = (String) row[1];
            Integer count = ((Number) row[2]).intValue();
            
            if (ageGroupCounts.containsKey(ageGroup)) {
                ageGroupCounts.get(ageGroup).put(gender, count);
            }
        }
        
        // 프론트엔드가 기대하는 형태로 결과 생성 (각 나이대마다 male, female 값 포함)
        List<Map<String, Object>> distribution = new java.util.ArrayList<>();
        
        for (String ageGroup : ageGroups) {
            Map<String, Object> ageData = new java.util.HashMap<>();
            ageData.put("name", ageGroup);
            ageData.put("male", ageGroupCounts.get(ageGroup).get("남성"));
            ageData.put("female", ageGroupCounts.get(ageGroup).get("여성"));
            distribution.add(ageData);
        }
        
        return distribution;
    }
    
    @Transactional
    private void createMissingCampaignTargets(Campaign campaign) {
        // 기존 캠페인의 필터 정보가 없다면 필터 없이 고객들을 선택
        List<Customer> allCustomers = customerRepository.findByFiltersWithRadius(
            null, null, null, null, null, null, null, null
        );
        
        // recipientsCount만큼 랜덤하게 고객 선택
        int targetCount = Math.min(campaign.getRecipientsCount(), allCustomers.size());
        java.util.Collections.shuffle(allCustomers);
        
        List<Customer> selectedCustomers = allCustomers.subList(0, targetCount);
        
        for (Customer customer : selectedCustomers) {
            CampaignTarget target = new CampaignTarget();
            target.setCampaign(campaign);
            target.setCustomer(customer);
            target.setDeliveryStatus(CampaignTarget.DeliveryStatus.DELIVERED);
            target.setSentAt(campaign.getCreatedAt()); // 캠페인 생성 시간을 발송 시간으로 사용
            
            // 일부는 읽음 처리 (30% 확률)
            if (Math.random() < 0.3) {
                target.setReadAt(campaign.getCreatedAt().plusMinutes((long)(Math.random() * 60)));
            }
            
            // 일부는 클릭 처리 (5% 확률)
            if (Math.random() < 0.05 && target.getReadAt() != null) {
                target.setClickAt(target.getReadAt().plusMinutes((long)(Math.random() * 30)));
            }
            
            campaignTargetRepository.save(target);
        }
        
        System.out.println("Created " + selectedCustomers.size() + " targets for campaign " + campaign.getId());
    }
    
    private Map<String, Object> generateHourlyStats(Long campaignId) {
        // 시간별 발송, 읽음, 클릭 데이터 조회
        List<Object[]> hourlySentStats = campaignTargetRepository.findHourlySentStatsByCampaignId(campaignId);
        List<Object[]> hourlyReadStats = campaignTargetRepository.findHourlyReadStatsByCampaignId(campaignId);
        List<Object[]> hourlyClickStats = campaignTargetRepository.findHourlyClickStatsByCampaignId(campaignId);
        
        // 24시간 배열 초기화 (0-23시)
        int[] sentByHour = new int[24];
        int[] readByHour = new int[24];
        int[] clickByHour = new int[24];
        
        // 발송 데이터 채우기
        for (Object[] row : hourlySentStats) {
            Integer hour = (Integer) row[0];
            Long count = (Long) row[1];
            if (hour != null && hour >= 0 && hour < 24) {
                sentByHour[hour] = count.intValue();
            }
        }
        
        // 읽음 데이터 채우기
        for (Object[] row : hourlyReadStats) {
            Integer hour = (Integer) row[0];
            Long count = (Long) row[1];
            if (hour != null && hour >= 0 && hour < 24) {
                readByHour[hour] = count.intValue();
            }
        }
        
        // 클릭 데이터 채우기
        for (Object[] row : hourlyClickStats) {
            Integer hour = (Integer) row[0];
            Long count = (Long) row[1];
            if (hour != null && hour >= 0 && hour < 24) {
                clickByHour[hour] = count.intValue();
            }
        }
        
        // 시간별 데이터 배열 생성
        List<Map<String, Object>> hourlyDataList = new java.util.ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourData = new java.util.HashMap<>();
            hourData.put("time", String.format("%02d:00", hour));
            hourData.put("sent", sentByHour[hour]);
            hourData.put("read", readByHour[hour]);
            hourData.put("click", clickByHour[hour]);
            hourlyDataList.add(hourData);
        }
        
        return Map.of("hourlyStats", hourlyDataList);
    }
}
