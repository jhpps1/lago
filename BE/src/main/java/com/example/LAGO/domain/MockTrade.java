package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 모의거래 엔티티 (완전 구현)
 * 지침서 명세 MOCK_TRADE 테이블과 완전 일치
 */
@Entity
@Table(name = "mock_trade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mock_trade_seq")
    @SequenceGenerator(name = "mock_trade_seq", sequenceName = "mock_trade_trade_id_seq", allocationSize = 1)
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(name = "account_id")
    private Long accountId; // 계좌 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account; // 계좌 정보

    @Column(name = "stock_id")
    private Integer stockId; // STOCK_INFO 테이블의 stock_info_id 참조
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", referencedColumnName = "stock_info_id", insertable = false, updatable = false)
    private StockInfo stockInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "buy_sell", nullable = false)
    private TradeType tradeType; // 거래구분 (BUY/SELL)

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 거래 수량

    @Column(name = "price", nullable = false)
    private Integer price; // 거래 단가

    @Column(name = "trade_at", nullable = false)
    private LocalDateTime tradeAt; // 거래 시간

    @Column(name = "is_quiz")
    private Boolean isQuiz; // 퀴즈 거래 여부

    // 호환성을 위한 계산 필드 (DB에는 없지만 기존 코드에서 사용)
    @Transient
    private Integer commission; // 수수료 계산용

    public Integer getCommission() {
        if (commission != null) return commission;
        // 기본 수수료 계산 로직 (거래금액의 0.015%)
        if (price != null && quantity != null) {
            return Math.max((int) (getTotalAmount() * 0.00015), 1);
        }
        return 0;
    }
    
    // 총 거래 금액 계산 메소드 (price * quantity 기반)
    public Integer getTotalAmount() {
        if (price != null && quantity != null) {
            return price * quantity;
        }
        return 0;
    }

    @PrePersist
    protected void onCreate() {
        if (tradeAt == null) {
            tradeAt = LocalDateTime.now();
        }
        if (isQuiz == null) {
            isQuiz = false;
        }
    }
}
