package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * 이 인터페이스는 사용자 인증, 회원가입, 사용자 조회 등의 
 * 인증 관련 핵심 비즈니스 로직을 정의합니다.
 * 
 * 주요 기능:
 * - 사용자 회원가입 (이메일 중복 체크, 비밀번호 암호화)
 * - JWT 기반 로그인 (토큰 생성)
 * - 현재 사용자 정보 조회
 * 
 * @author KT 위치 문자 서비스 팀
 * @version 1.0
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
