package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ticks_3m")
@Getter
@Setter
@NoArgsConstructor
public class Ticks3m {

    @EmbeddedId
    private Ticks3mId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stockInfoId") // Ticks3mId의 stockInfoId와 매핑
    @JoinColumn(name = "stock_info_id", insertable = false, updatable = false)
    private StockInfo stockInfo;

    @Column(name = "open_price")
    private Integer openPrice;

    @Column(name = "high_price")
    private Integer highPrice;

    @Column(name = "low_price")
    private Integer lowPrice;

    @Column(name = "close_price")
    private Integer closePrice;

    @Column(name = "volume")
    private Long volume;

    // 편의 메서드
    public OffsetDateTime getBucket() {
        return id.getBucket();
    }

    public Integer getStockInfoId() {
        return id.getStockInfoId();
    }
}