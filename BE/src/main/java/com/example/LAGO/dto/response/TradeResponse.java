package com.example.LAGO.dto.response;

import com.example.LAGO.domain.TradeType;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 거래 응답 DTO
 * 
 * 지침서 명세: API 명세서 기준, 파라미터/반환 구조 임의 변경 금지
 * 연동된 EC2 DB MOCK_TRADE 테이블 결과 반환
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "거래 응답 정보")
public class TradeResponse {

    /**
     * 응답 성공 여부
     */
    @Schema(description = "거래 성공 여부", example = "true")
    private boolean success;

    /**
     * 거래 ID (MOCK_TRADE 테이블 trade_id)
     */
    @Schema(description = "거래 ID", example = "12345")
    private Long tradeId;

    /**
     * 사용자 ID
     */
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    /**
     * 종목 코드
     */
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    /**
     * 종목명
     */
    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;

    /**
     * 거래 타입
     */
    @Schema(description = "거래 타입", example = "BUY")
    private TradeType tradeType;

    /**
     * 거래 수량
     */
    @Schema(description = "거래 수량", example = "10")
    private Integer quantity;

    /**
     * 거래 단가
     */
    @Schema(description = "거래 단가", example = "75000")
    private Integer price;

    /**
     * 총 거래 금액
     */
    @Schema(description = "총 거래 금액", example = "750000")
    private Integer totalAmount;

    /**
     * 수수료
     */
    @Schema(description = "수수료", example = "1500")
    private Integer commission;

    /**
     * 세금
     */
    @Schema(description = "세금", example = "0")
    private Integer tax;

    /**
     * 거래 시간
     */
    @Schema(description = "거래 시간")
    private LocalDateTime tradeAt;

    /**
     * 응답 메시지
     */
    @Schema(description = "응답 메시지", example = "거래가 성공적으로 처리되었습니다.")
    private String message;

    /**
     * 에러 코드 (실패시)
     */
    @Schema(description = "에러 코드", example = "INSUFFICIENT_BALANCE")
    private String errorCode;

    /**
     * 계좌 잔고 (거래 후)
     */
    @Schema(description = "거래 후 계좌 잔고", example = "1249000")
    private Integer remainingBalance;

    /**
     * 성공 응답 생성 팩토리 메서드
     * 지침서 명세: 예외처리/Validation 코드 필수
     */
    public static TradeResponse success(Long tradeId, Long userId, String stockCode, String stockName,
                                      TradeType tradeType, Integer quantity, Integer price, Integer totalAmount,
                                      Integer commission, Integer tax, Integer remainingBalance, String message) {
        return TradeResponse.builder()
                .success(true)
                .tradeId(tradeId)
                .userId(userId)
                .stockCode(stockCode)
                .stockName(stockName)
                .tradeType(tradeType)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .commission(commission)
                .tax(tax)
                .remainingBalance(remainingBalance)
                .tradeAt(LocalDateTime.now())
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성 팩토리 메서드
     */
    public static TradeResponse failure(String errorCode, String message) {
        return TradeResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .tradeAt(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (상세 정보 포함)
     */
    public static TradeResponse failure(Long userId, String stockCode, TradeType tradeType, 
                                      String errorCode, String message) {
        return TradeResponse.builder()
                .success(false)
                .userId(userId)
                .stockCode(stockCode)
                .tradeType(tradeType)
                .errorCode(errorCode)
                .message(message)
                .tradeAt(LocalDateTime.now())
                .build();
    }
}
