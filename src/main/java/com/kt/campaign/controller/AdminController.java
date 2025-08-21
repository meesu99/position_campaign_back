package com.kt.campaign.controller;

import com.kt.campaign.entity.Customer;
import com.kt.campaign.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * 관리자 전용 REST API를 처리하는 컨트롤러
 * 
 * 이 컨트롤러는 관리자만 접근할 수 있는 고객 관리 API를 제공합니다.
 * Spring Security에서 ADMIN 역할을 가진 사용자만 접근이 허용됩니다.
 * 
 * 주요 기능:
 * - 고객 목록 조회 (ID 순 정렬, 페이징)
 * - 고객 정보 수정
 * - 고객 삭제
 * - 필터링 기능 (성별, 지역, 나이)
 * 
 * 특별 기능:
 * - ID 순서대로 정렬하여 관리 편의성 향상
 * - 위치 정보 (PostGIS) 처리
 * - DTO 변환으로 JSON 직렬화 문제 해결
 * 
 * @author KT 위치 문자 서비스 팀
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final CustomerRepository customerRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    
    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers(@RequestParam(required = false) String gender,
                                        @RequestParam(required = false) String sido,
                                        @RequestParam(required = false) String sigungu,
                                        @RequestParam(required = false) Integer ageFrom,
                                        @RequestParam(required = false) Integer ageTo,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Customer> customers = customerRepository.findByFilters(
                gender, sido, sigungu, ageFrom, ageTo, pageable
            );
            
            // DTO로 변환하여 JSON 직렬화 문제 방지
            List<Map<String, Object>> customerDtos = customers.getContent().stream()
                .map(customer -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", customer.getId());
                    dto.put("name", customer.getName());
                    dto.put("gender", customer.getGender());
                    dto.put("birthYear", customer.getBirthYear());
                    dto.put("phone", customer.getPhone());
                    dto.put("roadAddress", customer.getRoadAddress());
                    dto.put("detailAddress", customer.getDetailAddress());
                    dto.put("postalCode", customer.getPostalCode());
                    dto.put("sido", customer.getSido());
                    dto.put("sigungu", customer.getSigungu());
                    dto.put("lat", customer.getLat());
                    dto.put("lng", customer.getLng());
                    dto.put("createdAt", customer.getCreatedAt());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("customers", customerDtos);
            response.put("totalPages", customers.getTotalPages());
            response.put("totalElements", customers.getTotalElements());
            response.put("currentPage", customers.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, Object> request) {
        try {
            Customer customer = new Customer();
            customer.setName((String) request.get("name"));
            customer.setGender((String) request.get("gender"));
            customer.setBirthYear((Integer) request.get("birthYear"));
            customer.setPhone((String) request.get("phone"));
            customer.setRoadAddress((String) request.get("roadAddress"));
            customer.setDetailAddress((String) request.get("detailAddress"));
            customer.setPostalCode((String) request.get("postalCode"));
            customer.setSido((String) request.get("sido"));
            customer.setSigungu((String) request.get("sigungu"));
            
            Double lat = ((Number) request.get("lat")).doubleValue();
            Double lng = ((Number) request.get("lng")).doubleValue();
            customer.setLat(lat);
            customer.setLng(lng);
            
            // PostGIS Point 생성
            Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
            point.setSRID(4326);
            customer.setGeom(point);
            
            Customer saved = customerRepository.save(customer);
            
            return ResponseEntity.ok(Map.of(
                "message", "고객이 생성되었습니다.",
                "customer", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request) {
        try {
            Customer customer = customerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));
            
            customer.setName((String) request.get("name"));
            customer.setGender((String) request.get("gender"));
            customer.setBirthYear((Integer) request.get("birthYear"));
            customer.setPhone((String) request.get("phone"));
            customer.setRoadAddress((String) request.get("roadAddress"));
            customer.setDetailAddress((String) request.get("detailAddress"));
            customer.setPostalCode((String) request.get("postalCode"));
            customer.setSido((String) request.get("sido"));
            customer.setSigungu((String) request.get("sigungu"));
            
            if (request.get("lat") != null && request.get("lng") != null) {
                Double lat = ((Number) request.get("lat")).doubleValue();
                Double lng = ((Number) request.get("lng")).doubleValue();
                customer.setLat(lat);
                customer.setLng(lng);
                
                Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
                point.setSRID(4326);
                customer.setGeom(point);
            }
            
            Customer updated = customerRepository.save(customer);
            
            return ResponseEntity.ok(Map.of(
                "message", "고객 정보가 업데이트되었습니다.",
                "customer", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            customerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "고객이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
