package com.kt.campaign.controller;

import com.kt.campaign.config.JwtConfig;
import com.kt.campaign.entity.AppUser;
import com.kt.campaign.service.AuthServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 REST API를 처리하는 컨트롤러
 * 
 * 이 컨트롤러는 사용자 인증(로그인/회원가입/로그아웃) API를 제공합니다.
 * JWT 토큰을 쿠키로 관리하며, HttpOnly 속성으로 보안을 강화합니다.
 * 
 * 주요 기능:
 * - 회원가입: 이메일 중복 체크, 비밀번호 암호화
 * - 로그인: JWT 토큰 생성 및 쿠키 설정
 * - 로그아웃: 쿠키 삭제
 * - 현재 사용자 정보 조회
 * 
 * 보안 특징:
 * - JWT 토큰을 HttpOnly 쿠키로 저장
 * - CSRF 공격 방지를 위한 SameSite 설정
 * 
 * @author KT 위치 문자 서비스 팀
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthServiceInterface authService;
    private final JwtConfig jwtConfig;
    
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        try {
            AppUser user = authService.signup(
                request.get("email"),
                request.get("password"),
                request.get("businessNo"),
                request.get("companyName")
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "회원가입이 완료되었습니다.",
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "companyName", user.getCompanyName(),
                    "role", user.getRole()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        System.out.println("Login endpoint reached!");
        System.out.println("Request body: " + request);
        try {
            String token = authService.login(request.get("email"), request.get("password"));
            
            // HttpOnly 쿠키에 JWT 저장
            Cookie cookie = new Cookie(jwtConfig.getCookieName(), token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // 개발환경에서는 false
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtConfig.getExpiration() / 1000));
            response.addCookie(cookie);
            
            AppUser user = authService.getCurrentUser(request.get("email"));
            
            return ResponseEntity.ok(Map.of(
                "message", "로그인이 완료되었습니다.",
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "companyName", user.getCompanyName(),
                    "role", user.getRole(),
                    "points", user.getPoints()
                )
            ));
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtConfig.getCookieName(), null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String email) {
        try {
            // 인증되지 않은 경우
            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }
            
            AppUser user = authService.getCurrentUser(email);
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "companyName", user.getCompanyName(),
                "role", user.getRole(),
                "points", user.getPoints()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "인증 정보가 유효하지 않습니다."));
        }
    }
}
