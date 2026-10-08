package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전체 거래 내역 응답 DTO
 * 사용자의 모든 거래 내역을 반환하는 용도
 * 
 * @author LAGO 팀
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "거래 내역 정보")
public class TransactionHistoryResponse {

    @Schema(description = "거래 ID", example = "1")
    private Long tradeId;

    @Schema(description = "계좌 ID", example = "1")
    private Long accountId;

    @Schema(description = "주식명", example = "삼성전자", nullable = true)
    private String stockName;

    @Schema(description = "주식 코드", example = "005930", nullable = true)
    private String stockId;

    @Schema(description = "거래 수량", example = "2", nullable = true)
    private Integer quantity;

    @Schema(description = "매수/매도 구분", example = "BUY", allowableValues = {"BUY", "SELL"}, nullable = true)
    private String buySell;

    @Schema(description = "거래 가격 (mock_trade의 price)", example = "150000")
    private Integer price;

    @Schema(description = "거래 시간", example = "2025-06-20T22:31:00")
    private LocalDateTime tradeAt;

    @Schema(description = "퀴즈로 받은 값인지 여부", example = "false")
    private Boolean isQuiz;
}