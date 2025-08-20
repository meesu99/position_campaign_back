package com.kt.campaign.controller;

import com.kt.campaign.entity.Customer;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.entity.CampaignTarget;
import com.kt.campaign.repository.CustomerRepository;
import com.kt.campaign.repository.CampaignTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerRepository customerRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    
    @GetMapping("/{customerId}/messages")
    public ResponseEntity<?> getCustomerMessages(@PathVariable Long customerId) {
        try {
            // 고객 정보 조회
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 해당 고객에게 발송된 캠페인 타겟 조회
            List<CampaignTarget> targets = campaignTargetRepository.findByCustomerIdOrderBySentAtDesc(customerId);
            
            // 메시지 리스트 생성
            List<Map<String, Object>> messages = targets.stream()
                .filter(target -> target.getSentAt() != null) // 실제 발송된 것만
                .map(target -> {
                    Campaign campaign = target.getCampaign();
                    Map<String, Object> message = new HashMap<>();
                    message.put("id", target.getId());
                    message.put("title", campaign.getTitle());
                    message.put("messageText", campaign.getMessageText());
                    message.put("link", campaign.getLink());
                    message.put("companyName", campaign.getUser().getCompanyName());
                    message.put("sentAt", target.getSentAt());
                    message.put("readAt", target.getReadAt());
                    message.put("clickAt", target.getClickAt());
                    return message;
                })
                .collect(Collectors.toList());
            
            // 고객 정보 (마스킹)
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("id", customer.getId());
            customerInfo.put("maskedName", maskName(customer.getName()));
            customerInfo.put("maskedAddress", maskAddress(customer.getRoadAddress(), customer.getSido(), customer.getSigungu()));
            
            Map<String, Object> response = new HashMap<>();
            response.put("customer", customerInfo);
            response.put("messages", messages);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting customer messages: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "메시지를 불러오는 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/messages/{targetId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long targetId) {
        try {
            CampaignTarget target = campaignTargetRepository.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (target.getReadAt() == null) {
                target.setReadAt(LocalDateTime.now());
                campaignTargetRepository.save(target);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            System.err.println("Error marking message as read: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "읽음 처리 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/messages/{targetId}/click")
    public ResponseEntity<?> markAsClicked(@PathVariable Long targetId) {
        try {
            CampaignTarget target = campaignTargetRepository.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (target.getClickAt() == null) {
                target.setClickAt(LocalDateTime.now());
                // 클릭하면 자동으로 읽음 처리도
                if (target.getReadAt() == null) {
                    target.setReadAt(LocalDateTime.now());
                }
                campaignTargetRepository.save(target);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            System.err.println("Error marking message as clicked: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "클릭 처리 중 오류가 발생했습니다."));
        }
    }
    
    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        
        StringBuilder masked = new StringBuilder();
        masked.append(name.charAt(0));
        for (int i = 1; i < name.length() - 1; i++) {
            masked.append("*");
        }
        masked.append(name.charAt(name.length() - 1));
        
        return masked.toString();
    }
    
    private String maskAddress(String roadAddress, String sido, String sigungu) {
        if (sido != null && sigungu != null) {
            return sido + " " + sigungu;
        } else if (sido != null) {
            return sido;
        } else if (roadAddress != null && roadAddress.length() > 10) {
            return roadAddress.substring(0, 10) + "...";
        }
        return "주소 정보 없음";
    }
}
