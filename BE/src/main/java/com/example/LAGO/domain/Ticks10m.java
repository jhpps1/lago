package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ticks_10m")
@Getter
@Setter
@NoArgsConstructor
public class Ticks10m {

    @EmbeddedId
    private Ticks10mId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stockInfoId")
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

    public OffsetDateTime getBucket() {
        return id.getBucket();
    }

    public Integer getStockInfoId() {
        return id.getStockInfoId();
    }
}