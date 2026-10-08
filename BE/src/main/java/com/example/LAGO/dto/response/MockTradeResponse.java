package com.example.LAGO.dto.response;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 모의투자 거래 응답 DTO
 * 
 * 연동된 EC2 DB 기반:
 * - MOCK_TRADE 테이블 결과 반환
 * - ACCOUNT 테이블 업데이트 결과 포함
 * - 거래 성공/실패 정보와 상세 결과 제공
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "모의투자 거래 결과 정보")
public class MockTradeResponse {

    /**
     * 거래 성공 여부
     */
    @Schema(description = "거래 성공 여부", example = "true")
    private Boolean success;

    /**
     * 거래 ID
     */
    @Schema(description = "생성된 거래 ID", example = "12345")
    private Long tradeId;

    /**
     * 종목 코드
     */
    @Schema(description = "거래 종목 코드", example = "005930")
    private String stockCode;

    /**
     * 종목명
     */
    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;

    /**
     * 거래 수량
     */
    @Schema(description = "실제 거래된 수량", example = "10")
    private Integer quantity;

    /**
     * 거래 가격
     */
    @Schema(description = "실제 거래 가격", example = "75000")
    private Integer executedPrice;

    /**
     * 총 거래 금액
     */
    @Schema(description = "총 거래 금액 (수수료 포함)", example = "750150")
    private Integer totalAmount;

    /**
     * 총 거래 비용 (매수시 지출, 매도시 수입)
     */
    @Schema(description = "총 거래 비용", example = "750000")
    private Integer totalCost;

    /**
     * 수수료
     */
    @Schema(description = "거래 수수료", example = "150")
    private Integer commission;

    /**
     * 거래 후 잔액
     */
    @Schema(description = "거래 후 계좌 잔액", example = "9249850")
    private Integer remainingBalance;

    /**
     * 거래 타입
     */
    @Schema(description = "거래 타입", example = "BUY", allowableValues = {"BUY", "SELL"})
    private String tradeType;

    /**
     * 거래 일시
     */
    @Schema(description = "거래 실행 일시", example = "2025-08-06T14:30:00")
    private LocalDateTime tradeAt;

    /**
     * 에러 메시지 (실패시)
     */
    @Schema(description = "에러 메시지 (거래 실패시)", example = "잔액이 부족합니다")
    private String errorMessage;

    /**
     * 결과 메시지 (성공/실패 공통)
     */
    @Schema(description = "거래 결과 메시지", example = "매수 주문이 성공적으로 처리되었습니다")
    private String message;

    /**
     * 성공 응답 생성
     */
    public static MockTradeResponse success(Long tradeId, String stockCode, String stockName,
                                             Integer quantity, Integer executedPrice, Integer totalAmount,
                                             Integer commission, Integer remainingBalance, String tradeType) {
        return MockTradeResponse.builder()
            .success(true)
            .tradeId(tradeId)
            .stockCode(stockCode)
            .stockName(stockName)
            .quantity(quantity)
            .executedPrice(executedPrice)
            .totalAmount(totalAmount)
            .commission(commission)
            .remainingBalance(remainingBalance)
            .tradeType(tradeType)
            .tradeAt(LocalDateTime.now())
            .build();
    }

    /**
     * 실패 응답 생성
     */
    public static MockTradeResponse failure(String stockCode, String errorMessage) {
        return MockTradeResponse.builder()
            .success(false)
            .stockCode(stockCode)
            .errorMessage(errorMessage)
            .tradeAt(LocalDateTime.now())
            .build();
    }
}
