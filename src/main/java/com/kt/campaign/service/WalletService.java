package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.WalletTransaction;
import com.kt.campaign.repository.AppUserRepository;
import com.kt.campaign.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 지갑(포인트) 관련 비즈니스 로직을 처리하는 서비스 구현체
 * 
 * 이 서비스는 사용자의 포인트 충전, 차감, 잔액 조회 등의
 * 지갑 관련 핵심 비즈니스 로직을 구현합니다.
 * 
 * 주요 기능:
 * - 포인트 충전 및 거래 내역 생성
 * - 캠페인 발송을 위한 포인트 차감
 * - 실시간 잔액 계산 (거래 내역 합계 기반)
 * - 거래 내역 페이징 조회
 * 
 * 특별 기능:
 * - 실제 거래 내역 합계로 잔액 계산 (데이터 정합성 보장)
 * - 트랜잭션 처리로 데이터 일관성 유지
 * - 충전/차감 시 메타데이터 저장 (캠페인 ID 등)
 * 
 * 잔액 계산 방식:
 * - 충전(CHARGE): 양수 금액
 * - 차감(DEBIT): 음수 금액
 * - 현재 잔액 = 모든 거래 내역의 amount 합계
 * 
 * @author KT 위치 문자 서비스 팀
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService implements WalletServiceInterface {
    
    private final WalletTransactionRepository walletTransactionRepository;
    private final AppUserRepository appUserRepository;
    
    @Transactional
    public WalletTransaction charge(Long userId, Long amount, Map<String, Object> meta) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 실제 거래 내역 합계로 현재 잔액 계산
        Long currentBalance = walletTransactionRepository.calculateActualBalanceByUserId(userId);
        Long newBalance = currentBalance + amount;
        
        // 사용자 포인트 업데이트
        user.setPoints(newBalance);
        appUserRepository.save(user);
        
        // 거래 기록 생성
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setType(WalletTransaction.Type.CHARGE);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setMeta(meta);
        
        return walletTransactionRepository.save(transaction);
    }
    
    @Transactional
    public WalletTransaction debitForCampaign(Long userId, Long amount, Long campaignId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 실제 거래 내역 합계로 현재 잔액 계산
        Long currentBalance = walletTransactionRepository.calculateActualBalanceByUserId(userId);
        
        if (currentBalance < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        
        Long newBalance = currentBalance - amount;
        
        // 사용자 포인트 업데이트
        user.setPoints(newBalance);
        appUserRepository.save(user);
        
        // 거래 기록 생성
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setType(WalletTransaction.Type.DEBIT_CAMPAIGN);
        transaction.setAmount(-amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setMeta(Map.of("campaign_id", campaignId));
        
        return walletTransactionRepository.save(transaction);
    }
    
    public Long getCurrentBalance(Long userId) {
        // 실제 거래 내역 합계로 정확한 잔액 계산
        Long actualBalance = walletTransactionRepository.calculateActualBalanceByUserId(userId);
        Long maxBalanceAfter = walletTransactionRepository.findCurrentBalanceByUserId(userId);
        
        // AppUser.points와 동기화
        AppUser user = appUserRepository.findById(userId).orElse(null);
        if (user != null) {
            if (!actualBalance.equals(maxBalanceAfter)) {
                System.out.println("Balance inconsistency detected - Actual sum: " + actualBalance + ", Max balance_after: " + maxBalanceAfter);
            }
            
            if (!actualBalance.equals(user.getPoints())) {
                System.out.println("User.points mismatch detected - Actual balance: " + actualBalance + ", User.points: " + user.getPoints());
                // 실제 거래 합계로 AppUser.points 업데이트
                user.setPoints(actualBalance);
                appUserRepository.save(user);
            }
        }
        
        return actualBalance != null ? actualBalance : 0L;
    }
    
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
