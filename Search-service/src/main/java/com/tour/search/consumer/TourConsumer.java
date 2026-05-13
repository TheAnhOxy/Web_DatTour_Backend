package com.tour.search.consumer;

import com.tour.search.document.DepartureDoc;
import com.tour.search.document.TourDocument;
import com.tour.search.event.TourSearchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourConsumer {

    private final ElasticsearchOperations elasticsearchOperations;

    @KafkaListener(topics = "tour-search-topic", groupId = "search-group")
    public void consume(TourSearchEvent event) {
        log.info("Nhận dữ liệu tour từ Kafka: {}", event.getTourId());

        TourDocument doc = TourDocument.builder()
                .id(event.getTourId().toString())
                .title(event.getTitle())
                .destinations(event.getDestinations())
                .departures(event.getDepartures().stream()
                        .map(d -> {
                            DepartureDoc dd = new DepartureDoc();
                            dd.setId(d.getId());
                            dd.setStartDate(d.getStartDate());
                            dd.setEndDate(d.getEndDate());
                            return dd;
                        }).toList())
                .build();

        // Lưu vào Elasticsearch - Lệnh này sẽ tự tạo Index nếu chưa có
        elasticsearchOperations.save(doc);
    }
}