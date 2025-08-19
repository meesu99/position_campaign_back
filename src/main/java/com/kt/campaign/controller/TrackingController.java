package com.kt.campaign.controller;

import com.kt.campaign.entity.CampaignTarget;
import com.kt.campaign.repository.CampaignTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/t")
@RequiredArgsConstructor
public class TrackingController {
    
    private final CampaignTargetRepository campaignTargetRepository;
    
    @GetMapping("/r/{targetId}")
    public ResponseEntity<byte[]> trackRead(@PathVariable Long targetId) {
        try {
            CampaignTarget target = campaignTargetRepository.findById(targetId)
                    .orElse(null);
            
            if (target != null && target.getReadAt() == null) {
                target.setReadAt(LocalDateTime.now());
                campaignTargetRepository.save(target);
            }
            
            // 1x1 투명 픽셀 이미지 반환
            byte[] pixel = new byte[]{
                (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00,
                (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x01, (byte) 0x00,
                (byte) 0x3B
            };
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "image/gif");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            
            return new ResponseEntity<>(pixel, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/c/{targetId}")
    public ResponseEntity<Void> trackClick(@PathVariable Long targetId) {
        try {
            CampaignTarget target = campaignTargetRepository.findById(targetId)
                    .orElse(null);
            
            if (target != null) {
                if (target.getReadAt() == null) {
                    target.setReadAt(LocalDateTime.now());
                }
                if (target.getClickAt() == null) {
                    target.setClickAt(LocalDateTime.now());
                }
                campaignTargetRepository.save(target);
                
                // 캠페인 링크로 리다이렉트
                String redirectUrl = target.getCampaign().getLink();
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(redirectUrl))
                            .build();
                }
            }
            
            // 기본 리다이렉트 URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("https://shop.kt.com/"))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("https://shop.kt.com/"))
                    .build();
        }
    }
}
