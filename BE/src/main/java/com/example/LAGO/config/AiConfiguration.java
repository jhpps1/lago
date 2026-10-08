package com.example.LAGO.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * AI 관련 설정 클래스
 * 
 * 지침서 명세:
 * - Claude API 통신을 위한 JSON 파싱 설정
 * - 기존 서비스들을 위한 기본 RestTemplate Bean 제공
 * - Jackson ObjectMapper Bean 제공
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Configuration
@Slf4j
public class AiConfiguration {

    /**
     * FinBERT API 호출용 RestTemplate Bean (긴 타임아웃 설정)
     * (Claude API는 OkHttp를 직접 사용)
     * 
     * @return RestTemplate 인스턴스
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(30))  // 연결 타임아웃
            .setReadTimeout(Duration.ofMinutes(25))     // 읽기 타임아웃 25분 (FinBERT 처리 시간 고려)
            .build();
        
        log.info("FinBERT용 RestTemplate Bean 생성 완료 - 타임아웃: 연결 30초, 읽기 25분");
        return restTemplate;
    }

}
