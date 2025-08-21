package com.cloudpower.scheduler;

import com.cloudpower.scheduler.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "xxl.job.admin.addresses=http://localhost:8080/xxl-job-admin",
    "xxl.job.executor.port=0"
})
class UnifiedSchedulerApplicationTests {

    @Test
    void contextLoads() {
        // 测试Spring Boot应用上下文是否正常加载
    }

}