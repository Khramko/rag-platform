package com.example.document.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.document-uploaded}")
    private String documentUploadedTopic;

    @Value("${kafka.topics.document-deleted}")
    private String documentDeletedTopic;

    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder.name(documentUploadedTopic)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic documentDeletedTopic() {
        return TopicBuilder.name(documentDeletedTopic)
                .partitions(3).replicas(1).build();
    }

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
}