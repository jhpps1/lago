package com.example.LAGO.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;   // ← models 쪽만 사용
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}") private String appVersion;
    @Value("${server.port:8080}")  private String serverPort;
    @Value("${spring.profiles.active:dev}") private String activeProfile;

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())               // 서버 주소 설정
                .components(createComponents());
        //.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info createApiInfo() {
        return new Info()
                .title("라고할때 (LAGO) API Documentation")
                .description("...생략(기존 텍스트 유지)...")
                .version(appVersion)
                .contact(new Contact().name("D203팀 박준형")
                        .email("ssafy.d203@example.com")
                        .url("https://github.com/ssafy-d203"))
                .license(new License().name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    // ✅ 추천: 상대경로로 고정 — 접속한 도메인/포트를 그대로 사용
    private List<Server> createServers() {
        return List.of(new Server().url("/"));
    }

    private Components createComponents() {
        return new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer Token Authentication"));
    }

    // GroupedOpenApi 그룹화 제거 - 모든 API를 기본 그룹에서 표시
}
