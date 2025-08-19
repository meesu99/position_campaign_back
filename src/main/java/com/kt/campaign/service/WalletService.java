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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {
    
    private final WalletTransactionRepository walletTransactionRepository;
    private final AppUserRepository appUserRepository;
    
    @Transactional
    public WalletTransaction charge(Long userId, Long amount, Map<String, Object> meta) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Long currentBalance = getCurrentBalance(userId);
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
        
        Long currentBalance = getCurrentBalance(userId);
        
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
        Long balance = walletTransactionRepository.findCurrentBalanceByUserId(userId);
        return balance != null ? balance : 0L;
    }
    
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
