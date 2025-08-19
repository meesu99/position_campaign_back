package com.kt.campaign.controller;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.service.AuthService;
import com.kt.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignService campaignService;
    private final AuthService authService;
    
    @PostMapping("/preview")
    public ResponseEntity<?> previewCampaign(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> filters = (Map<String, Object>) request.get("filters");
            Map<String, Object> result = campaignService.previewCampaign(filters);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody Map<String, Object> request,
                                          @AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            
            Campaign campaign = campaignService.createCampaign(
                user,
                (String) request.get("title"),
                (String) request.get("messageText"),
                (String) request.get("link"),
                (Map<String, Object>) request.get("filters")
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "캠페인이 생성되었습니다.",
                "campaign", Map.of(
                    "id", campaign.getId(),
                    "title", campaign.getTitle(),
                    "status", campaign.getStatus(),
                    "recipientsCount", campaign.getRecipientsCount(),
                    "estimatedCost", campaign.getEstimatedCost()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getUserCampaigns(@AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            List<Campaign> campaigns = campaignService.getUserCampaigns(user.getId());
            
            return ResponseEntity.ok(Map.of("campaigns", campaigns));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<?> sendCampaign(@PathVariable Long id,
                                        @AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            campaignService.sendCampaign(id, user);
            
            return ResponseEntity.ok(Map.of("message", "캠페인 발송이 시작되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getCampaignStats(@PathVariable Long id,
                                            @AuthenticationPrincipal String email) {
        try {
            Map<String, Object> stats = campaignService.getCampaignStats(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
