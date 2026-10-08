package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 관심 종목 엔티티
 * EC2 데이터베이스 INTEREST 테이블과 완전 일치
 */
@Entity
@Table(name = "interest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Integer interestId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "stock_info_id", nullable = false)
    private Integer stockInfoId;
    
    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_info_id", insertable = false, updatable = false)
    private StockInfo stockInfo;
}