package com.kt.campaign.controller;

import com.kt.campaign.entity.Customer;
import com.kt.campaign.entity.Campaign;
import com.kt.campaign.entity.CampaignTarget;
import com.kt.campaign.repository.CustomerRepository;
import com.kt.campaign.repository.CampaignTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<?> getCustomerMessages(@PathVariable Long customerId) {
        try {
            // 트랜잭션 동기화 매니저를 통해 현재 트랜잭션 상태 확인
            System.out.println("Transaction active: " + TransactionSynchronizationManager.isActualTransactionActive());
            
            // JPA 1차 캐시 완전 클리어로 최신 데이터 보장
            entityManager.clear();
            
            // 고객 정보 조회
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 해당 고객에게 발송된 캠페인 타겟 조회 (네이티브 SQL 사용)
            entityManager.clear(); // 한 번 더 클리어
            
            // 최신 데이터를 확실히 가져오기 위해 flush 후 clear
            entityManager.flush();
            entityManager.clear();
            
            // 네이티브 SQL로 직접 조회 (JPA 캐시 완전 우회)
            List<Object[]> targetResults = entityManager.createNativeQuery(
                "SELECT ct.id, ct.sent_at, ct.read_at, ct.click_at, " +
                "c.title, c.message_text, c.link, u.company_name " +
                "FROM campaign_targets ct " +
                "JOIN campaigns c ON ct.campaign_id = c.id " +
                "JOIN app_users u ON c.user_id = u.id " +
                "WHERE ct.customer_id = ? AND ct.sent_at IS NOT NULL " +
                "ORDER BY ct.sent_at DESC"
            )
            .setParameter(1, customerId)
            .getResultList();
            
            System.out.println("Customer " + customerId + " messages loaded via native SQL. Count: " + targetResults.size());
            
            // 메시지 리스트 생성 (네이티브 SQL 결과 사용)
            List<Map<String, Object>> messages = targetResults.stream()
                .map(result -> {
                    Map<String, Object> message = new HashMap<>();
                    message.put("id", ((Number) result[0]).longValue()); // ct.id
                    message.put("sentAt", result[1]); // ct.sent_at
                    message.put("readAt", result[2]); // ct.read_at
                    message.put("clickAt", result[3]); // ct.click_at
                    message.put("title", result[4]); // c.title
                    message.put("messageText", result[5]); // c.message_text
                    message.put("link", result[6]); // c.link
                    message.put("companyName", result[7]); // u.company_name
                    
                    System.out.println("Message " + result[0] + " readAt from DB: " + result[2]);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> markAsRead(@PathVariable Long targetId) {
        try {
            // 먼저 현재 상태 확인
            List<Object[]> currentState = entityManager.createNativeQuery(
                "SELECT id, read_at, click_at FROM campaign_targets WHERE id = ?"
            )
            .setParameter(1, targetId)
            .getResultList();
            
            System.out.println("Current state for message " + targetId + ": " + 
                (currentState.isEmpty() ? "NOT FOUND" : 
                "read_at=" + currentState.get(0)[1] + ", click_at=" + currentState.get(0)[2]));
            
            // 네이티브 SQL로 직접 업데이트 (JPA 캐시 우회)
            LocalDateTime now = LocalDateTime.now();
            int updatedRows = entityManager.createNativeQuery(
                "UPDATE campaign_targets SET read_at = ? WHERE id = ? AND read_at IS NULL"
            )
            .setParameter(1, now)
            .setParameter(2, targetId)
            .executeUpdate();
            
            if (updatedRows > 0) {
                // 강제로 데이터베이스에 반영하고 캐시 완전 클리어
                entityManager.flush();
                entityManager.clear();
                
                System.out.println("Message " + targetId + " marked as read at: " + now + " (Updated rows: " + updatedRows + ")");
                return ResponseEntity.ok(Map.of("success", true, "updated", true));
            } else {
                // 이미 읽음 처리되었거나 존재하지 않는 메시지
                System.out.println("Message " + targetId + " was already read or doesn't exist (Updated rows: " + updatedRows + ")");
                return ResponseEntity.ok(Map.of("success", true, "updated", false));
            }
            
        } catch (Exception e) {
            System.err.println("Error marking message as read: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "읽음 처리 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/messages/{targetId}/click")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> markAsClicked(@PathVariable Long targetId) {
        try {
            // 네이티브 SQL로 직접 업데이트 (JPA 캐시 우회)
            LocalDateTime now = LocalDateTime.now();
            int updatedRows = entityManager.createNativeQuery(
                "UPDATE campaign_targets SET click_at = ?, read_at = COALESCE(read_at, ?) WHERE id = ? AND click_at IS NULL"
            )
            .setParameter(1, now)  // click_at
            .setParameter(2, now)  // read_at (if null)
            .setParameter(3, targetId)
            .executeUpdate();
            
            if (updatedRows > 0) {
                // 강제로 데이터베이스에 반영하고 캐시 완전 클리어
                entityManager.flush();
                entityManager.clear();
                
                System.out.println("Message " + targetId + " marked as clicked at: " + now + " (Updated rows: " + updatedRows + ")");
                return ResponseEntity.ok(Map.of("success", true, "updated", true));
            } else {
                // 이미 클릭 처리되었거나 존재하지 않는 메시지
                System.out.println("Message " + targetId + " was already clicked or doesn't exist");
                return ResponseEntity.ok(Map.of("success", true, "updated", false));
            }
            
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
