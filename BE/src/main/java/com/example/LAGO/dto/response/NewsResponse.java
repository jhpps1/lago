package com.example.LAGO.dto.response;

import com.example.LAGO.domain.News;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {
    
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String sentiment;
    private LocalDateTime publishedAt;
    private String type;
    
    public static NewsResponse from(News news) {
        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .summary(news.getSummary())
                .sentiment(news.getSentiment())
                .publishedAt(news.getPublishedAt())
                .type(news.getType())
                .build();
    }
}