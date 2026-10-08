package com.example.LAGO.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 투자 용어 DTO
 * INVESTMENT_TERM 테이블 데이터 전송용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "투자 용어 정보")
public class InvestmentTermDto {
    
    @Schema(description = "용어 ID", example = "1")
    private Integer termId;
    
    @Schema(description = "용어", example = "주가수익비율")
    private String term;
    
    @Schema(description = "정의", example = "Price Earnings Ratio, 주가를 주당순이익으로 나눈 비율")
    private String definition;
    
    @Schema(description = "설명", example = "기업의 가치를 평가하는 대표적인 지표로 PER이라고도 불립니다. 낮을수록 저평가된 주식으로 여겨집니다.")
    private String description;
    
    @Schema(description = "사용자가 이 용어를 아는지 여부 (userId 제공 시만)", example = "true")
    private Boolean knowStatus;

    /**
     * Entity -> DTO 변환 생성자
     *
     * @param investmentTerm 투자 용어 엔티티
     */
    public InvestmentTermDto(com.example.LAGO.domain.InvestmentTerm investmentTerm) {
        this.termId = investmentTerm.getTermId();
        this.term = investmentTerm.getTerm();
        this.definition = investmentTerm.getDefinition();
        this.description = investmentTerm.getDescription();
    }
}