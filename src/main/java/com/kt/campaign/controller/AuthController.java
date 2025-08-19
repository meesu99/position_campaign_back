package com.kt.campaign.controller;

import com.kt.campaign.config.JwtConfig;
import com.kt.campaign.entity.AppUser;
import com.kt.campaign.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
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
