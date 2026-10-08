package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;
    
    @Column(columnDefinition = "TEXT")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String sentiment;  // 감정 분석 결과
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(columnDefinition = "TEXT")
    private String type;  // 뉴스 타입
    
    // Getter method for ID (for backward compatibility)
    public Long getId() {
        return newsId;
    }
    
    @PrePersist
    protected void onCreate() {
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}