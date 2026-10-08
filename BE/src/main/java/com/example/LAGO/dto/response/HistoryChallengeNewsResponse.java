package com.example.LAGO.dto.response;

import com.example.LAGO.domain.HistoryChallengeNews;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryChallengeNewsResponse {

    @Schema(description = "챌린지 뉴스 ID", example = "1")
    private Integer challengeNewsId;

    @Schema(description = "챌린지 ID", example = "1")
    private Integer challengeId;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "발행일시", example = "2020-08-09 15:10:00")
    private LocalDateTime publishedAt;

    public HistoryChallengeNewsResponse(HistoryChallengeNews entity) {
        this.challengeNewsId = entity.getChallengeNewsId();
        this.challengeId = entity.getChallengeId();
        this.title = entity.getTitle();
        this.content = entity.getContent();
        this.publishedAt = entity.getPublishedAt();
    }
}