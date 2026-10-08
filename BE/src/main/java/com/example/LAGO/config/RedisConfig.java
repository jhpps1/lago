package com.example.LAGO.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@Configuration
public class RedisConfig {
    
    /**
     * Redis Template 설정
     * String 키/값 및 Stream 처리를 위한 설정
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String serializer 사용
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // 키 serialization
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // 값 serialization
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        // Stream 메시지의 필드와 값을 위한 serializer
        template.setDefaultSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Object 타입을 위한 RedisTemplate 설정
     * 토큰 관리 및 임시 데이터 저장용
     */
    @Bean
    public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(stringSerializer); // Hash 값은 String으로

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 압축된 바이너리 데이터 저장을 위한 RedisTemplate
     * TickChunk 압축 배치 저장용
     */
    @Bean("binaryRedisTemplate")
    public RedisTemplate<String, byte[]> binaryRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value는 바이트 배열 그대로 저장 (압축된 데이터)
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setHashValueSerializer(RedisSerializer.byteArray());

        // 기본 직렬화는 바이트 배열
        template.setDefaultSerializer(RedisSerializer.byteArray());

        template.afterPropertiesSet();
        return template;
    }
}