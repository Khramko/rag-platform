package com.example.embedding.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic embeddingCreatedTopic() {
        return TopicBuilder.name("embedding.created")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic embeddingFailedTopic() {
        return TopicBuilder.name("embedding.failed")
                .partitions(3).replicas(1).build();
    }

    /**
     * 3 попытки с паузой 1 секунда. После — сообщение пропускается
     * (без DLT). Это лучше, чем 0 попыток по умолчанию.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
}