package com.kt.campaign.repository;

import com.kt.campaign.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(wt.balanceAfter), 0) FROM WalletTransaction wt WHERE wt.user.id = :userId")
    Long findCurrentBalanceByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt WHERE wt.user.id = :userId")
    Long calculateActualBalanceByUserId(@Param("userId") Long userId);
}
