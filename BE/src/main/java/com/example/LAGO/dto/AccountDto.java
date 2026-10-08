package com.example.LAGO.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 계좌 조회 응답 DTO
 * DB/Entity(Account) 필드와 1:1 매핑 - 명세서 기준
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "계좌 조회 응답 DTO")
public class AccountDto {

	@Schema(description = "계좌 PK")
	private Long accountId;

	@Schema(description = "사용자 PK")
	private Long userId;

	@Schema(description = "보유 현금")
	private Integer balance;

	@Schema(description = "총 자산")
	private Integer totalAsset;

	@Schema(description = "수익")
	private Integer profit;

	@Schema(description = "수익률")
	private Double profitRate;

	@Schema(description = "계좌구분(0:모의투자, 1:역사챌린지)")
	private Integer type;
}
