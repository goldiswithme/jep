package com.cloudpower.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 统一外部系统接口调度服务主应用类
 * Unified External System Interface Scheduler Application
 * 
 * 该服务提供统一的外部系统接口调度功能，支持：
 * - HTTP、WebService、WebSocket、MQTT等多种协议
 * - 接口调用编排（登录获取token后调用业务接口）
 * - 定时任务配置
 * - 数据处理与格式转换
 * - MQ消息发送和统一保存API
 * - 接口调用日志记录
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class UnifiedSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedSchedulerApplication.class, args);
    }
}