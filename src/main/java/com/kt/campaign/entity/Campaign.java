package com.kt.campaign.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    
    private String title;
    
    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;
    
    private String link;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> filters;
    
    @Column(name = "price_per_recipient")
    private Integer pricePerRecipient;
    
    @Column(name = "estimated_cost")
    private Long estimatedCost;
    
    @Column(name = "final_cost")
    private Long finalCost;
    
    @Column(name = "recipients_count")
    private Integer recipientsCount;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum Status {
        DRAFT, SENDING, COMPLETED, FAILED
    }
}
