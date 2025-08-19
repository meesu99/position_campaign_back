package com.kt.campaign.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String gender;
    
    @Column(name = "birth_year")
    private Integer birthYear;
    
    private String phone;
    
    @Column(name = "road_address")
    private String roadAddress;
    
    @Column(name = "detail_address")
    private String detailAddress;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    private String sido;
    
    private String sigungu;
    
    private Double lat;
    
    private Double lng;
    
    @Column(name = "geom", columnDefinition = "geography(Point,4326)")
    private Point geom;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
