package com.kt.campaign.controller;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.entity.Customer;
import com.kt.campaign.repository.AppUserRepository;
import com.kt.campaign.repository.CustomerRepository;
import com.kt.campaign.service.CampaignServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 캠페인 관련 REST API를 처리하는 컨트롤러
 * 
 * 이 컨트롤러는 캠페인의 생성, 조회, 발송, 통계 등의 API 엔드포인트를 제공합니다.
 * 인증된 사용자만 접근할 수 있으며, JWT 토큰을 통해 사용자를 식별합니다.
 * 
 * 주요 기능:
 * - 캠페인 미리보기 (고객 수, 예상 비용 계산)
 * - 캠페인 생성 및 발송
 * - 캠페인 목록 조회 및 상세 통계
 * - 대시보드용 통합 통계 제공
 * 
 * @author KT 위치 문자 서비스 팀
 */
@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignServiceInterface campaignService;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    public ResponseEntity<?> getUserCampaigns(@AuthenticationPrincipal String email) {
        try {
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            List<Campaign> campaigns = campaignService.getUserCampaigns(user.getId());
            
            // DTO로 변환하여 JSON 직렬화 문제 방지
            List<Map<String, Object>> campaignDtos = campaigns.stream()
                .map(campaign -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", campaign.getId());
                    dto.put("title", campaign.getTitle());
                    dto.put("messageText", campaign.getMessageText());
                    dto.put("link", campaign.getLink());
                    dto.put("status", campaign.getStatus().name());
                    dto.put("pricePerRecipient", campaign.getPricePerRecipient());
                    dto.put("estimatedCost", campaign.getEstimatedCost());
                    dto.put("finalCost", campaign.getFinalCost());
                    dto.put("recipientsCount", campaign.getRecipientsCount());
                    dto.put("createdAt", campaign.getCreatedAt());
                                            return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(campaignDtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal String email) {
        try {
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Map<String, Object> stats = campaignService.getDashboardStats(user.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createCampaign(@AuthenticationPrincipal String email,
                                          @RequestBody Map<String, Object> request) {
        try {
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            String title = (String) request.get("title");
            String messageText = (String) request.get("messageText");
            String link = (String) request.get("link");
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            
            Campaign campaign = campaignService.createCampaign(user, title, messageText, link, filters);
            return ResponseEntity.ok(Map.of(
                "message", "캠페인이 생성되었습니다.",
                "campaign", campaign
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<?> sendCampaign(@AuthenticationPrincipal String email,
                                        @PathVariable Long id) {
        try {
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            campaignService.sendCampaign(id, user);
            return ResponseEntity.ok(Map.of("message", "캠페인이 발송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getCampaignStats(@AuthenticationPrincipal String email,
                                            @PathVariable Long id) {
        try {
            AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Map<String, Object> stats = campaignService.getCampaignStats(id, user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/preview")
    public ResponseEntity<?> previewCampaign(@AuthenticationPrincipal String email,
                                           @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            Map<String, Object> preview = campaignService.previewCampaign(filters);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 캠페인 생성을 위한 고객 데이터 조회 (로그인된 사용자 모두 접근 가능)
     */
    @GetMapping("/customers")
    public ResponseEntity<?> getCustomersForCampaign(@RequestParam(required = false) String gender,
                                                   @RequestParam(required = false) String sido,
                                                   @RequestParam(required = false) String sigungu,
                                                   @RequestParam(required = false) Integer ageFrom,
                                                   @RequestParam(required = false) Integer ageTo,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "1000") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Customer> customers = customerRepository.findByFilters(
                gender, sido, sigungu, ageFrom, ageTo, pageable
            );
            
            // DTO로 변환하여 JSON 직렬화 문제 방지 및 개인정보 보호
            List<Map<String, Object>> customerDtos = customers.getContent().stream()
                .map(customer -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", customer.getId());
                    // 개인정보는 마스킹하거나 제외
                    dto.put("gender", customer.getGender());
                    dto.put("birthYear", customer.getBirthYear());
                    dto.put("sido", customer.getSido());
                    dto.put("sigungu", customer.getSigungu());
                    dto.put("lat", customer.getLat());
                    dto.put("lng", customer.getLng());
                    dto.put("createdAt", customer.getCreatedAt());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "customers", customerDtos,
                "totalPages", customers.getTotalPages(),
                "totalElements", customers.getTotalElements(),
                "currentPage", customers.getNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}