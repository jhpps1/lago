package com.example.LAGO.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 퀴즈 DTO
 * QUIZ 테이블 데이터 전송용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "퀴즈 정보")
public class QuizDto {
    
    @Schema(description = "퀴즈 ID", example = "1")
    @JsonProperty("quiz_id")
    private Integer quizId;
    
    @Schema(description = "문제", example = "주식의 PER이 높다는 것은 그 기업의 성장 가능성을 높게 본다는 뜻이다?")
    @JsonProperty("question")
    private String question;
    
    @Schema(description = "정답", example = "true")
    @JsonProperty("answer")
    private Boolean answer;
    
    @Schema(description = "해설", example = "PER이 높다는 것은 주가가 주당순이익 대비 높게 형성되어 있다는 의미입니다. 이는 투자자들이 미래 성장을 기대하고 있음을 나타내지만, 동시에 주가가 과대평가되었을 가능성도 있습니다.")
    @JsonProperty("explanation")
    private String explanation;
    
    @Schema(description = "투자 용어 ID", example = "1")
    @JsonProperty("term_id")
    private Integer termId;

    /**
     * Entity -> DTO 변환 생성자
     *
     * @param quiz 퀴즈 엔티티
     */
    public QuizDto(com.example.LAGO.domain.Quiz quiz) {
        this.quizId = quiz.getQuizId();
        this.question = quiz.getQuestion();
        this.answer = quiz.getAnswer();
        this.explanation = quiz.getExplanation();
        this.termId = quiz.getTermId();
    }
}