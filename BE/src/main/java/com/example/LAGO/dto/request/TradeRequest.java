package com.example.LAGO.dto.request;

import com.example.LAGO.domain.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 매매 요청
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매매 요청")
public class TradeRequest {

    @Schema(description = "사용자 ID", example = "123", required = true)
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    private Long userId;

    @Schema(description = "종목 코드", example = "005930", required = true)
    @NotBlank(message = "종목 코드는 필수입니다")
    private String stockCode;

    @Schema(description = "거래 타입 (BUY/SELL)", example = "BUY", required = true)
    @NotNull(message = "거래 타입은 필수입니다")
    private TradeType tradeType;

    @Schema(description = "거래 수량", example = "10", required = true)
    @NotNull(message = "거래 수량은 필수입니다")
    @Positive(message = "거래 수량은 양수여야 합니다")
    private Integer quantity;

    @Schema(description = "거래 단가", example = "75000", required = true)
    @NotNull(message = "거래 단가는 필수입니다")
    @Positive(message = "거래 단가는 양수여야 합니다")
    private Integer price;

    @Schema(description = "계좌 타입 (0=실시간모의투자, 1=역사챌린지, 2=자동매매봇)", example = "0", required = true)
    @NotNull(message = "계좌 타입은 필수입니다")
    private Integer accountType;
}
