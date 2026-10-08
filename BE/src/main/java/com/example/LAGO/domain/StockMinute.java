package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_minute")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMinute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_min_id")
    private Integer stockMinId;

    // 외래키: STOCK_INFO 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_info_id")
    private StockInfo stockInfo;

    @Column(name = "date", nullable = false)
    private java.time.LocalDateTime date;

    @Column(name = "open_price", nullable = false)
    private Integer openPrice;

    @Column(name = "high_price", nullable = false)
    private Integer highPrice;

    @Column(name = "low_price", nullable = false)
    private Integer lowPrice;

    @Column(name = "close_price", nullable = false)
    private Integer closePrice;

    @Column(name = "volume", nullable = false)
    private Integer volume;
}
