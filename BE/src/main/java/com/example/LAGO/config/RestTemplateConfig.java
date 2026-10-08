//package com.example.LAGO.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestTemplate;
//
///**
// * RestTemplateConfig
// *
// * 외부 REST API 호출에 사용할 RestTemplate Bean을 스프링 컨테이너에 등록하는 설정 클래스입니다.
// *  => KisAuthClient에 사용할 클래스
// * <p>RestTemplate:
// *  - Spring에서 제공하는 동기식 HTTP 클라이언트
// *  - GET, POST, PUT, DELETE 등 다양한 HTTP 메서드 호출 지원
// *  - JSON/XML 등의 요청·응답 변환 지원
// *
// * <p>이 설정을 통해 애플리케이션 전역에서 RestTemplate 객체를
// *  직접 생성(new)하지 않고 주입(@Autowired, 생성자 주입)받아 재사용할 수 있습니다.
// *  AiConfiguration에 이미 restTemplate 관련된게 있어서 일단 그걸 사용하고 이건 주석처리
// */
//
//@Configuration
//public class RestTemplateConfig {
//    // RestTemplace Bean 등록
//    @Bean
//    public RestTemplate restTemplate() {
//        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
//        factory.setConnectTimeout(5_000);
//        factory.setReadTimeout(10_000);
//        return new RestTemplate(factory);
//    }
//}
