package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_day")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDay {

/**
 * 주식 일봉 데이터 엔티티
 * 연동된 EC2 DB STOCK_DAY 테이블과 완전 일치
 * 테이블 구조:
 * - stock_day_id: PK
 * - stock_info_id: FK (STOCK_INFO)
 * - date: 거래일
 * - open_price, high_price, low_price, close_price: OHLC 데이터
 * - fluctuation_rate: 등락률
 * - volume: 거래량

    /**
     * 주식 일봉 데이터 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_day_id")
    private Integer stockDayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_info_id", referencedColumnName = "stock_info_id", insertable = false, updatable = false)
    private StockInfo stockInfo;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "open_price", nullable = false)
    private Integer openPrice;

    @Column(name = "high_price", nullable = false)
    private Integer highPrice;

    @Column(name = "low_price", nullable = false)
    private Integer lowPrice;

    @Column(name = "close_price", nullable = false)
    private Integer closePrice;

    @Column(name = "fluctuation_rate")
    private Float fluctuationRate;

    @Column(name = "volume", nullable = false)
    private Integer volume;
}

