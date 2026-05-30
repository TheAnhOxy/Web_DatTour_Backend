package com.tour.booking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null) {
            redisHost = "127.0.0.1";
        }
        config.useSingleServer().setAddress("redis://" + redisHost + ":6379");
        // Thêm dòng này
        config.setCodec(new org.redisson.client.codec.StringCodec());
        return Redisson.create(config);
    }
}