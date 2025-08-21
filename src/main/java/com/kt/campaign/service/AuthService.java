package com.kt.campaign.service;

import com.kt.campaign.entity.AppUser;
import com.kt.campaign.repository.AppUserRepository;
import com.kt.campaign.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
