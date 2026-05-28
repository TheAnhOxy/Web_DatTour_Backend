package com.tour.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUris;

    @Override
    public ClientConfiguration clientConfiguration() {
        String hostAndPort = extractHostAndPort(elasticsearchUris);
        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .build();
    }

    private String extractHostAndPort(String uriValue) {
        if (uriValue == null || uriValue.isBlank()) {
            return "localhost:9200";
        }

        String firstUri = uriValue.split(",")[0].trim();
        if (firstUri.contains("://")) {
            java.net.URI parsed = java.net.URI.create(firstUri);
            String host = parsed.getHost();
            int port = parsed.getPort() > 0 ? parsed.getPort() : 9200;
            if (host != null && !host.isBlank()) {
                return host + ":" + port;
            }
        }

        return firstUri;
    }

    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
                Arrays.asList(
                        new LongToLocalDateTimeConverter(),
                        new LocalDateTimeToLongConverter()
                )
        );
    }

    @ReadingConverter
    static class LongToLocalDateTimeConverter implements Converter<Long, LocalDateTime> {
        @Override
        public LocalDateTime convert(Long source) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(source), ZoneId.systemDefault());
        }
    }

    @WritingConverter
    static class LocalDateTimeToLongConverter implements Converter<LocalDateTime, Long> {
        @Override
        public Long convert(LocalDateTime source) {
            return source.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }
}