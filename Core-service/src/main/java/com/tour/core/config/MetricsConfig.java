//package com.tour.core.config;
//
//import io.micrometer.core.instrument.Clock;
//import io.micrometer.prometheus.PrometheusConfig;      // 💡 Thay đổi package import ở đây
//import io.micrometer.prometheus.PrometheusMeterRegistry; // 💡 Thay đổi package import ở đây
//import io.prometheus.client.CollectorRegistry;           // 💡 Phiên bản 1.12.x dùng bản này
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MetricsConfig {
//
//    @Bean
//    public PrometheusMeterRegistry prometheusMeterRegistry() {
//        // Khởi tạo Registry chuẩn đét theo đúng thế hệ Micrometer 1.12.5
//        return new PrometheusMeterRegistry(
//                PrometheusConfig.DEFAULT,
//                CollectorRegistry.defaultRegistry,
//                Clock.SYSTEM
//        );
//    }
//}