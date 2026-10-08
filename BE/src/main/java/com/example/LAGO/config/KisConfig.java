package com.example.LAGO.config;

import com.example.LAGO.kis.KisAuthClient;
import com.example.LAGO.kis.KisPropertiesConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;


// 사용자(계정)별 KisAuthClient 빈 생성 (매매 테스트를 위해 임시 비활성화)
@Configuration
@EnableConfigurationProperties(KisPropertiesConfig.class)
public class KisConfig {
    @Bean("kisAuth_userA")
    public KisAuthClient kisAuthClientUserA(RestTemplate rest, KisPropertiesConfig props) {
        var a = props.byName("userA");
        return new KisAuthClient(rest, a.getAppKey(), a.getAppSecret());
    }

    @Bean("kisAuth_userB")
    public KisAuthClient kisAuthUserB(RestTemplate rest, KisPropertiesConfig props) {
        var a = props.byName("userB");
        return new KisAuthClient(rest, a.getAppKey(), a.getAppSecret());
    }
}
