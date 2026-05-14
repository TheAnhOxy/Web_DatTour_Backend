package com.tour.core.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DotenvLoader {

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("Core-service")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String value = System.getenv(entry.getKey());
            if (value == null || value.isBlank()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }
}
