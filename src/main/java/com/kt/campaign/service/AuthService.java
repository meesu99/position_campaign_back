package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.repository.AppUserRepository;
import com.kt.campaign.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 및 사용자 관리 서비스 구현체
 * 
 * 이 서비스는 사용자 회원가입, 로그인, 사용자 정보 조회 등의
 * 인증 관련 핵심 비즈니스 로직을 구현합니다.
 * 
 * 주요 기능:
 * - 사용자 회원가입 (이메일 중복 체크, 비밀번호 암호화)
 * - JWT 기반 로그인 (토큰 생성 및 반환)
 * - 현재 인증된 사용자 정보 조회
 * 
 * 보안 특징:
 * - BCrypt를 이용한 비밀번호 암호화
 * - JWT 토큰 생성 및 관리
 * - 이메일 중복 검증
 * 
 * @author KT 위치 문자 서비스 팀
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthServiceInterface {
    
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public AppUser signup(String email, String password, String businessNo, String companyName) {
        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setBusinessNo(businessNo);
        user.setCompanyName(companyName);
        user.setRole(AppUser.Role.USER);
        
        return appUserRepository.save(user);
    }
    
    public String login(String email, String password) {
        System.out.println("Login attempt for email: " + email);
        
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("User not found for email: " + email);
                    return new IllegalArgumentException("존재하지 않는 사용자입니다.");
                });
        
        System.out.println("User found: " + user.getEmail() + " with role: " + user.getRole());
        System.out.println("Input password: '" + password + "'");
        System.out.println("Input password length: " + password.length());
        System.out.println("Stored hash: " + user.getPasswordHash());
        
        // 새로운 해시 생성해서 비교
        String newHash = passwordEncoder.encode(password);
        System.out.println("New hash for comparison: " + newHash);
        
        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        System.out.println("Password matches: " + matches);
        
        if (!matches) {
            System.out.println("Password mismatch for user: " + email);
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        System.out.println("Login successful for user: " + email);
        return jwtUtil.generateToken(email, user.getRole().name());
    }
    
    public AppUser getCurrentUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
