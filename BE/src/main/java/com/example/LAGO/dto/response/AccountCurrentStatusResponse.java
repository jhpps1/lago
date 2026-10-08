package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 계좌 현재 상황 응답 DTO
 * 프론트에서 실시간 계산을 위한 계좌 현황 정보
 */
@Data
@Builder
@Schema(description = "계좌 현재 상황 응답 (프론트 실시간 계산용)")
public class AccountCurrentStatusResponse {

    @Schema(description = "계좌 ID", example = "1")
    private Long accountId;

    @Schema(description = "보유 현금 (잔액)", example = "5000000")
    private Integer balance;

    @Schema(description = "계좌 수익률 (%)", example = "15.5")
    private Double profitRate;

    @Schema(description = "보유 종목 목록")
    private List<CurrentHoldingInfo> holdings;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 닉네임", example = "투자고수")
    private String nickname;

    @Schema(description = "투자 성향", example = "aggressive")
    private String personality;

    @Data
    @Builder
    @Schema(description = "현재 보유 종목 정보")
    public static class CurrentHoldingInfo {

        @Schema(description = "종목 코드", example = "005930")
        private String stockCode;

        @Schema(description = "종목명", example = "삼성전자")
        private String stockName;

        @Schema(description = "보유 수량", example = "100")
        private Integer quantity;

        @Schema(description = "총 매수 금액", example = "7500000")
        private Integer totalPurchaseAmount;
    }
}