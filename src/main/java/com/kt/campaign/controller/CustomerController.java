package com.kt.campaign.controller;

import com.kt.campaign.entity.Customer;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.entity.CampaignTarget;
import com.kt.campaign.repository.CustomerRepository;
import com.kt.campaign.repository.CampaignTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 고객 메시지 확인 및 상호작용 API를 처리하는 컨트롤러
 * 
 * 이 컨트롤러는 고객이 받은 캠페인 메시지를 확인하고 상호작용하는 API를 제공합니다.
 * 인증 없이 접근 가능하며, 고객 ID로 메시지를 조회할 수 있습니다.
 * 
 * 주요 기능:
 * - 고객별 받은 메시지 목록 조회
 * - 메시지 읽음 처리 (readAt 업데이트)
 * - 메시지 링크 클릭 처리 (clickAt 업데이트)
 * 
 * 특별 기능:
 * - JPA 1차 캐시 관리로 실시간 상태 업데이트 반영
 * - 트랜잭션 분리 (REQUIRES_NEW)로 즉시 커밋
 * - EntityManager를 이용한 캐시 클리어
 * 
 * @author KT 위치 문자 서비스 팀
 */
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerRepository customerRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @GetMapping("/{customerId}/messages")
    @Transactional(readOnly = true)
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
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable Long targetId) {
        try {
            CampaignTarget target = campaignTargetRepository.findById(targetId).orElse(null);
            if (target == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (target.getReadAt() == null) {
                target.setReadAt(LocalDateTime.now());
                campaignTargetRepository.save(target);
                // 엔티티 매니저 캐시 새로고침
                entityManager.flush();
                entityManager.clear();
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
    @Transactional
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
                // 엔티티 매니저 캐시 새로고침
                entityManager.flush();
                entityManager.clear();
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
