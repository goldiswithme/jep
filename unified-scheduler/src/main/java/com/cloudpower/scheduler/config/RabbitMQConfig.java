package com.cloudpower.scheduler.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 * RabbitMQ Configuration
 */
@Configuration
public class RabbitMQConfig {

    @Value("${data-processing.persistence.mq.exchange:unified.scheduler.data}")
    private String exchangeName;

    @Value("${data-processing.persistence.mq.routing-key:processed.data}")
    private String routingKey;

    /**
     * 声明数据处理交换机
     */
    @Bean
    public DirectExchange dataProcessingExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /**
     * 声明数据处理队列
     */
    @Bean
    public Queue dataProcessingQueue() {
        return QueueBuilder.durable(exchangeName + ".queue").build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding dataProcessingBinding() {
        return BindingBuilder.bind(dataProcessingQueue())
                .to(dataProcessingExchange())
                .with(routingKey);
    }

    /**
     * RabbitMQ模板配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setExchange(exchangeName);
        rabbitTemplate.setRoutingKey(routingKey);
        return rabbitTemplate;
    }

    /**
     * 消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}