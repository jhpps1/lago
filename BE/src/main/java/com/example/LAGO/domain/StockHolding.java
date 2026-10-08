package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 보유주식 엔티티
 * DB 스키마 완전 준수: stock_holding 테이블
 * - 기본 필드: DB 컬럼과 1:1 매핑
 * - 계산 필드: @Transient로 비즈니스 로직용
 */
@Entity
@Table(name = "stock_holding")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id")
    private Long holdingId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Column(name = "stock_info_id", nullable = false)
    private Integer stockInfoId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_info_id", insertable = false, updatable = false)
    private StockInfo stockInfo;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 보유 수량

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice; // 총 매수 금액

    // ====== 호환성을 위한 계산 필드들 (DB에 저장되지 않음) ======
    @Transient
    private String stockCode; // stockInfo에서 가져옴

    @Transient
    private Integer averagePrice; // totalPrice / quantity로 계산

    @Transient
    private Integer totalCost; // totalPrice와 동일

    @Transient
    private Integer currentValue; // 현재 주가 * 수량

    @Transient
    private Integer profitLoss; // currentValue - totalPrice

    @Transient
    private Float profitLossRate; // profitLoss / totalPrice * 100

    @Transient
    private java.time.LocalDateTime firstPurchaseDate; // 별도 관리 필요

    @Transient
    private java.time.LocalDateTime lastTradeDate; // 별도 관리 필요

    // ====== 호환성을 위한 getter 메서드들 ======
    public String getStockCode() {
        if (stockCode != null) return stockCode;
        if (stockInfo != null) return stockInfo.getCode();
        return null;
    }

    public Integer getAveragePrice() {
        if (averagePrice != null) return averagePrice;
        if (quantity != null && quantity > 0 && totalPrice != null) {
            return totalPrice / quantity;
        }
        return 0;
    }

    public Integer getTotalCost() {
        if (totalCost != null) return totalCost;
        return totalPrice != null ? totalPrice : 0;
    }

    public Integer getCurrentValue() {
        return currentValue != null ? currentValue : 0;
    }

    public Integer getProfitLoss() {
        return profitLoss != null ? profitLoss : 0;
    }

    public Float getProfitLossRate() {
        return profitLossRate != null ? profitLossRate : 0.0f;
    }

    public java.time.LocalDateTime getFirstPurchaseDate() {
        return firstPurchaseDate;
    }

    public java.time.LocalDateTime getLastTradeDate() {
        return lastTradeDate;
    }

    // ====== 비즈니스 로직 메서드들 ======
    public void updateCurrentValue(Integer currentPrice) {
        if (currentPrice != null && quantity != null) {
            this.currentValue = currentPrice * quantity;
            this.profitLoss = this.currentValue - getTotalCost();
            if (getTotalCost() > 0) {
                this.profitLossRate = ((float) this.profitLoss / getTotalCost()) * 100;
            } else {
                this.profitLossRate = 0.0f;
            }
        }
    }

    public void setLastTradeDate(java.time.LocalDateTime lastTradeDate) {
        this.lastTradeDate = lastTradeDate;
    }

    public void setFirstPurchaseDate(java.time.LocalDateTime firstPurchaseDate) {
        this.firstPurchaseDate = firstPurchaseDate;
    }
}