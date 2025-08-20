package com.kt.campaign.config;

import com.kt.campaign.entity.*;
import com.kt.campaign.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;

    private final WalletTransactionRepository walletTransactionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 관리자 계정 강제 업데이트 또는 생성
        updateOrCreateAdminUser();
        
        // 일반 사용자 계정 강제 업데이트 또는 생성
        updateOrCreateRegularUser();
        
        // 데이터가 이미 있으면 나머지 초기화하지 않음
        if (userRepository.count() > 2) {
            return;
        }

        initializeCustomers();
        initializeCampaigns();
        initializeWalletTransactions();
        initializeChatMessages();
    }

    private void updateOrCreateAdminUser() {
        System.out.println("Updating or creating admin user...");
        
        AppUser admin = userRepository.findByEmail("admin@example.com").orElse(null);
        
        if (admin == null) {
            // 새로 생성
            admin = new AppUser();
            admin.setEmail("admin@example.com");
            admin.setBusinessNo("123-45-67890");
            admin.setCompanyName("Admin Company");
            admin.setPoints(100000L);
            admin.setRole(AppUser.Role.ADMIN);
            admin.setCreatedAt(LocalDateTime.now());
            System.out.println("Creating new admin user...");
        } else {
            System.out.println("Updating existing admin user password...");
        }
        
        // 비밀번호는 항상 새로 해시해서 설정
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        userRepository.save(admin);
        System.out.println("Admin user password updated to: admin123");
    }
    
    private void updateOrCreateRegularUser() {
        System.out.println("Updating or creating regular user...");
        
        AppUser user = userRepository.findByEmail("user@example.com").orElse(null);
        
        if (user == null) {
            // 새로 생성
            user = new AppUser();
            user.setEmail("user@example.com");
            user.setBusinessNo("987-65-43210");
            user.setCompanyName("User Company");
            user.setPoints(50000L);
            user.setRole(AppUser.Role.USER);
            user.setCreatedAt(LocalDateTime.now());
            System.out.println("Creating new regular user...");
        } else {
            System.out.println("Updating existing regular user password...");
        }
        
        // 비밀번호는 항상 새로 해시해서 설정
        user.setPasswordHash(passwordEncoder.encode("user123"));
        userRepository.save(user);
        System.out.println("Regular user password updated to: user123");
    }

    private void createAdminUser() {
        System.out.println("Creating admin user...");
        AppUser admin = new AppUser();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setBusinessNo("123-45-67890");
        admin.setCompanyName("Admin Company");
        admin.setPoints(100000L);
        admin.setRole(AppUser.Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
        System.out.println("Admin user created successfully!");
    }

    private void initializeUsers() {
        // 관리자 계정
        AppUser admin = new AppUser();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setBusinessNo("123-45-67890");
        admin.setCompanyName("Admin Company");
        admin.setPoints(100000L);
        admin.setRole(AppUser.Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);

        // 일반 사용자 계정
        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("user123"));
        user.setBusinessNo("987-65-43210");
        user.setCompanyName("User Company");
        user.setPoints(50000L);
        user.setRole(AppUser.Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void initializeCustomers() {
        // 서울 시청 근처 고객들
        createCustomer("김민수", "M", 1985, "010-1234-5678", "서울특별시 중구 세종대로 110", "시청 앞", "04524", "서울특별시", "중구", 37.5665, 126.9780);
        createCustomer("박지영", "F", 1990, "010-2345-6789", "서울특별시 강남구 테헤란로 152", "강남역 근처", "06236", "서울특별시", "강남구", 37.4979, 127.0276);
        createCustomer("이철수", "M", 1978, "010-3456-7890", "서울특별시 마포구 홍익로 94", "홍대입구역", "04039", "서울특별시", "마포구", 37.5511, 126.9230);
        createCustomer("최영희", "F", 1982, "010-4567-8901", "서울특별시 송파구 올림픽로 300", "잠실역 근처", "05551", "서울특별시", "송파구", 37.5133, 127.1028);
        createCustomer("장동건", "M", 1995, "010-5678-9012", "경기도 성남시 분당구 판교역로 235", "판교테크노밸리", "13494", "경기도", "성남시 분당구", 37.3925, 127.1107);
        createCustomer("김영미", "F", 1987, "010-6789-0123", "경기도 고양시 일산동구 중앙로 1036", "일산신도시", "10380", "경기도", "고양시 일산동구", 37.6566, 126.7695);
        createCustomer("이상호", "M", 1993, "010-7890-1234", "서울특별시 영등포구 여의도동 11", "여의도역", "07328", "서울특별시", "영등포구", 37.5219, 126.9245);
        createCustomer("박현정", "F", 1980, "010-8901-2345", "서울특별시 서초구 강남대로 200", "교대역", "06526", "서울특별시", "서초구", 37.4949, 127.0144);
        createCustomer("정민석", "M", 1991, "010-9012-3456", "인천광역시 연수구 송도과학로 123", "송도국제도시", "21984", "인천광역시", "연수구", 37.3891, 126.6453);
        createCustomer("김수연", "F", 1988, "010-0123-4567", "서울특별시 종로구 종로 69", "종각역", "03155", "서울특별시", "종로구", 37.5700, 126.9830);
    }

    private void createCustomer(String name, String gender, int birthYear, String phone, String roadAddress, String detailAddress, String postalCode, String sido, String sigungu, double lat, double lng) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setGender(gender);
        customer.setBirthYear(birthYear);
        customer.setPhone(phone);
        customer.setRoadAddress(roadAddress);
        customer.setDetailAddress(detailAddress);
        customer.setPostalCode(postalCode);
        customer.setSido(sido);
        customer.setSigungu(sigungu);
        customer.setLat(lat);
        customer.setLng(lng);
        customer.setCreatedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    private void initializeCampaigns() {
        AppUser user = userRepository.findByEmail("user@example.com").orElse(null);
        if (user == null) return;

        // 완료된 캠페인
        Campaign campaign1 = new Campaign();
        campaign1.setUser(user);
        campaign1.setTitle("갤럭시 폴드7 최저가 판매");
        campaign1.setMessageText("🔥 갤럭시 폴드7 최저가 특가! 지금 바로 확인하세요!");
        campaign1.setLink("https://shop.kt.com/");
        Map<String, Object> filters1 = new HashMap<>();
        filters1.put("gender", "F");
        filters1.put("ageRange", new int[]{25, 35});
        Map<String, String> region1 = new HashMap<>();
        region1.put("sido", "서울특별시");
        filters1.put("region", region1);
        campaign1.setFilters(filters1);
        campaign1.setPricePerRecipient(70);
        campaign1.setEstimatedCost(7000L);
        campaign1.setFinalCost(7000L);
        campaign1.setRecipientsCount(10);
        campaign1.setStatus(Campaign.Status.COMPLETED);
        campaign1.setCreatedAt(LocalDateTime.now());
        campaignRepository.save(campaign1);

        // 드래프트 캠페인
        Campaign campaign2 = new Campaign();
        campaign2.setUser(user);
        campaign2.setTitle("KT 5G 요금제 이벤트");
        campaign2.setMessageText("KT 5G 무제한 요금제로 갈아타고 혜택 받아가세요!");
        campaign2.setLink("https://shop.kt.com/5g");
        Map<String, Object> filters2 = new HashMap<>();
        filters2.put("gender", "M");
        filters2.put("ageRange", new int[]{30, 40});
        campaign2.setFilters(filters2);
        campaign2.setPricePerRecipient(50);
        campaign2.setEstimatedCost(5000L);
        campaign2.setRecipientsCount(10);
        campaign2.setStatus(Campaign.Status.DRAFT);
        campaign2.setCreatedAt(LocalDateTime.now());
        campaignRepository.save(campaign2);
    }

    private void initializeWalletTransactions() {
        AppUser user = userRepository.findByEmail("user@example.com").orElse(null);
        if (user == null) return;

        // 충전 거래
        WalletTransaction charge = new WalletTransaction();
        charge.setUser(user);
        charge.setType(WalletTransaction.Type.CHARGE);
        charge.setAmount(50000L);
        charge.setBalanceAfter(50000L);
        Map<String, Object> chargeMeta = new HashMap<>();
        chargeMeta.put("method", "credit_card");
        chargeMeta.put("card_last4", "1234");
        charge.setMeta(chargeMeta);
        charge.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(charge);

        // 캠페인 차감 거래
        WalletTransaction debit = new WalletTransaction();
        debit.setUser(user);
        debit.setType(WalletTransaction.Type.DEBIT_CAMPAIGN);
        debit.setAmount(-7000L);
        debit.setBalanceAfter(43000L);
        Map<String, Object> debitMeta = new HashMap<>();
        debitMeta.put("campaign_id", 1);
        debitMeta.put("recipients", 10);
        debitMeta.put("unit_price", 70);
        debit.setMeta(debitMeta);
        debit.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(debit);
    }

    private void initializeChatMessages() {
        AppUser user = userRepository.findByEmail("user@example.com").orElse(null);
        if (user == null) return;

        // 캠페인 완료 알림
        ChatMessage msg1 = new ChatMessage();
        msg1.setUser(user);
        msg1.setFromAdmin(true);
        msg1.setText("갤럭시 폴드7 최저가 판매 캠페인이 성공적으로 발송되었습니다.");
        msg1.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(msg1);

        // 일반 알림
        ChatMessage msg2 = new ChatMessage();
        msg2.setUser(user);
        msg2.setFromAdmin(true);
        msg2.setText("🎉 KT 쇼핑몰에서 새로운 이벤트가 시작되었습니다!");
        msg2.setLink("https://shop.kt.com/events");
        msg2.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(msg2);
    }
}
