package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface AuthServiceInterface {
    
    /**
     * 새로운 사용자 회원가입
     */
    AppUser signup(String email, String password, String businessNo, String companyName);
    
    /**
     * 사용자 로그인 및 JWT 토큰 생성
     */
    String login(String email, String password);
    
    /**
     * 이메일로 현재 사용자 정보 조회
     */
    AppUser getCurrentUser(String email);
}
