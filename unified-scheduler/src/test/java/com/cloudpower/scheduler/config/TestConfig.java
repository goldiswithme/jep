package com.cloudpower.scheduler.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;
import org.mockito.Mockito;

/**
 * 测试配置类
 * 提供模拟的外部依赖Bean
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public RestTemplate mockRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
}