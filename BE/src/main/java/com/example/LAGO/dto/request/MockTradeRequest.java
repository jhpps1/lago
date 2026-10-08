package com.example.LAGO.dto.request;

import com.example.LAGO.domain.TradeType;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 모의투자 거래 요청 DTO
 * 
 * 연동된 EC2 DB 기반:
 * - USERS 테이블: 사용자 정보
 * - STOCK_INFO 테이블: 종목 정보
 * - MOCK_TRADE 테이블: 거래 내역
 * - ACCOUNT 테이블: 계좌 정보
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "모의투자 거래 요청 정보")
public class MockTradeRequest {

    /**
     * 종목 코드
     */
    @NotBlank(message = "종목 코드는 필수입니다")
    @Schema(description = "거래 종목 코드", example = "005930")
    private String stockCode;

    /**
     * 거래 수량
     */
    @NotNull(message = "거래 수량은 필수입니다")
    @Positive(message = "거래 수량은 1 이상이어야 합니다")
    @Schema(description = "거래 수량", example = "10")
    private Integer quantity;

    /**
     * 거래 가격 (매수/매도 시 사용)
     */
    @Schema(description = "거래 가격 (현재가로 거래시 null 가능)", example = "75000")
    private Integer price;

    /**
     * 주문 타입 (시장가/지정가)
     */
    @Builder.Default
    @Schema(description = "주문 타입", example = "MARKET", allowableValues = {"MARKET", "LIMIT"})
    private String orderType = "MARKET";

    /**
     * 거래 타입 (매수/매도)
     */
    @NotNull(message = "거래 타입은 필수입니다")
    @Schema(description = "거래 타입", example = "BUY")
    private TradeType tradeType;

    /**
     * 요청 검증
     */
    public boolean isValidBuyRequest() {
        return stockCode != null && !stockCode.trim().isEmpty() 
               && quantity != null && quantity > 0
               && TradeType.BUY.equals(tradeType);
    }

    /**
     * 매도 요청 검증
     */
    public boolean isValidSellRequest() {
        return stockCode != null && !stockCode.trim().isEmpty() 
               && quantity != null && quantity > 0
               && TradeType.SELL.equals(tradeType);
    }
}
