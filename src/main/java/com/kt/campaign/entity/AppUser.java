package com.kt.campaign.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 애플리케이션 사용자 엔티티
 * 
 * 이 엔티티는 KT 위치 문자 서비스의 사용자 정보를 저장합니다.
 * 
 * 주요 속성:
 * - 사용자 기본 정보 (이메일, 비밀번호, 회사명, 사업자번호)
 * - 역할 관리 (USER, ADMIN)
 * - 포인트 잔액 (실시간 계산)
 * - 계정 생성/수정 시간 추적
 * 
 * 역할 정의:
 * - USER: 일반 사용자 (캠페인 생성/발송 가능)
 * - ADMIN: 관리자 (고객 관리만 가능)
 * 
 * 연관 관계:
 * - Campaign: 사용자가 생성한 캠페인들 (1:N)
 * - WalletTransaction: 사용자의 포인트 거래 내역들 (1:N)
 * 
 * @author KT 위치 문자 서비스 팀
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "business_no", nullable = false)
    private String businessNo;
    
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(nullable = false)
    private Long points = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum Role {
        USER, ADMIN
    }
}
