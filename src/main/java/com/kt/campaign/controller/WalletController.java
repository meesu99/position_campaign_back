package com.kt.campaign.controller;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.entity.WalletTransaction;
import com.kt.campaign.service.AuthService;
import com.kt.campaign.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {
    
    private final WalletService walletService;
    private final AuthService authService;
    
    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody Map<String, Object> request,
                                  @AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            Long amount = ((Number) request.get("amount")).longValue();
            Map<String, Object> meta = (Map<String, Object>) request.get("meta");
            
            WalletTransaction transaction = walletService.charge(user.getId(), amount, meta);
            
            return ResponseEntity.ok(Map.of(
                "message", "충전이 완료되었습니다.",
                "transaction", Map.of(
                    "id", transaction.getId(),
                    "amount", transaction.getAmount(),
                    "balanceAfter", transaction.getBalanceAfter(),
                    "createdAt", transaction.getCreatedAt()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/balance")
    public ResponseEntity<?> getCurrentBalance(@AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            Long balance = walletService.getCurrentBalance(user.getId());
            
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/ledger")
    public ResponseEntity<?> getTransactionHistory(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @AuthenticationPrincipal String email) {
        try {
            AppUser user = authService.getCurrentUser(email);
            Pageable pageable = PageRequest.of(page, size);
            Page<WalletTransaction> transactions = walletService.getTransactionHistory(user.getId(), pageable);
            
            return ResponseEntity.ok(Map.of(
                "transactions", transactions.getContent(),
                "totalPages", transactions.getTotalPages(),
                "totalElements", transactions.getTotalElements(),
                "currentPage", transactions.getNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
