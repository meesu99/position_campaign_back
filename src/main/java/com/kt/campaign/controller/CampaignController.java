package com.kt.campaign.controller;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.entity.Customer;
import com.kt.campaign.repository.AppUserRepository;
import com.kt.campaign.repository.CustomerRepository;
import com.kt.campaign.service.CampaignService;
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

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignService campaignService;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    
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
            Map<String, Object> stats = campaignService.getCampaignStats(id);
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