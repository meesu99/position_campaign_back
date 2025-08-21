package com.kt.campaign.service;

import com.kt.campaign.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 지갑(포인트) 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface WalletServiceInterface {
    
    /**
     * 포인트 충전
     */
    WalletTransaction charge(Long userId, Long amount, Map<String, Object> meta);
    
    /**
     * 캠페인 발송을 위한 포인트 차감
     */
    WalletTransaction debitForCampaign(Long userId, Long amount, Long campaignId);
    
    /**
     * 현재 포인트 잔액 조회
     */
    Long getCurrentBalance(Long userId);
    
    /**
     * 거래 내역 조회 (페이징)
     */
    Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable);
}
