package com.tour.booking.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Consumer với Dead Letter Queue (DLQ).
 *
 * Cơ chế hoạt động:
 * - Nếu Consumer gặp lỗi (Exception) khi xử lý 1 tin nhắn,
 *   hệ thống sẽ thử lại (Retry) tối đa 3 lần với khoảng chờ 2 giây.
 * - Nếu vẫn thất bại sau 3 lần, tin nhắn được chuyển vào Topic DLQ
 *   theo quy ước: <tên-topic-gốc>.DLT (Dead Letter Topic).
 *   VD: payment-completed-topic -> payment-completed-topic.DLT
 * - Các tin nhắn bình thường khác KHÔNG bị ảnh hưởng (không bị tắc nghẽn).
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * KafkaListenerContainerFactory được cấu hình với DLQ.
     *
     * @param kafkaTemplate dùng để ghi tin nhắn lỗi vào DLT topic
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // === CẤU HÌNH DLQ ===
        // DeadLetterPublishingRecoverer: Khi tin nhắn bị lỗi vượt số lần Retry,
        // nó sẽ tự động chuyển vào Topic "<tên-topic-gốc>.DLT"
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> {
                    // Log chi tiết lý do tin nhắn bị đẩy vào DLQ
                    log.error("[DLQ] Tin nhắn thất bại sau 3 lần thử. Topic gốc: {}, Key: {}, Lỗi: {}",
                            record.topic(), record.key(), exception.getMessage());
                    // Gửi vào topic DLT: <topic>.DLT (Dead Letter Topic)
                    return new org.apache.kafka.common.TopicPartition(
                            record.topic() + ".DLT", record.partition());
                });

        // === CẤU HÌNH RETRY ===
        // FixedBackOff(interval_ms, maxAttempts): Thử lại 3 lần, cách nhau 2 giây
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));

        // Bỏ qua một số lỗi không cần Retry (lỗi do dữ liệu sai format, không phải lỗi hệ thống)
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
