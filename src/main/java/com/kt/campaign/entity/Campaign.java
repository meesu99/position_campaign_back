package com.kt.campaign.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 캠페인 엔티티
 * 
 * 이 엔티티는 위치 기반 문자 캠페인의 정보를 저장합니다.
 * 
 * 주요 속성:
 * - 캠페인 기본 정보 (제목, 메시지 내용, 링크)
 * - 상태 관리 (초안, 대기, 진행중, 완료, 취소)
 * - 비용 정보 (예상 비용, 최종 비용, 수신자당 가격)
 * - 필터 조건 (JSON 형태로 저장)
 * 
 * 연관 관계:
 * - AppUser: 캠페인을 생성한 사용자 (N:1)
 * - CampaignTarget: 캠페인 발송 대상자들 (1:N)
 * 
 * @author KT 위치 문자 서비스 팀
 */
@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    
    private String title;
    
    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;
    
    private String link;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> filters;
    
    @Column(name = "price_per_recipient")
    private Integer pricePerRecipient;
    
    @Column(name = "estimated_cost")
    private Long estimatedCost;
    
    @Column(name = "final_cost")
    private Long finalCost;
    
    @Column(name = "recipients_count")
    private Integer recipientsCount;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum Status {
        DRAFT, SENDING, COMPLETED, FAILED
    }
}
