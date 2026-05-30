package com.tour.search.consumer;

import com.tour.search.document.DepartureDoc;
import com.tour.search.document.TourDocument;
import com.tour.search.event.TourSearchEvent;
import com.tour.search.index.TourIndex;
import com.tour.search.service.TourSearchService;
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
    private final TourSearchService tourSearchService;

    @KafkaListener(topics = "tour-sync-topic", groupId = "search-group")
    public void listenTourSync(TourSearchEvent event) {
        log.info("=> [Kafka Consumer] Nhận yêu cầu đồng bộ cho Tour ID: {}", event.getTourId());

        try {
            if (event.getTourId() == null) return;

            TourIndex tourIndex = TourIndex.builder()
                    .id(String.valueOf(event.getTourId()))
                    .title(event.getTitle())
                    .slug(event.getSlug())
                    .basePrice(event.getBasePrice())
                    .status("active")
                    .destinations(event.getDestinations())

                    .pickupTime(event.getPickupTime())
                    .isHot(event.getIsHot())
                    .coverImageUrl(event.getCoverImageUrl())
                    .categoryName(event.getCategoryName())
                    .durationDays(event.getDurationDays())
                    .region(event.getRegion())
                    .departureId(event.getDepartureId())
                    .departureStartDate(event.getDepartureStartDate())
                    .pickupName(event.getPickupName())
                    .pickupAddress(event.getPickupAddress())
                    .rating(event.getRating())
                    .reviewCount(event.getReviewCount())
                    .transportationType(event.getTransportationType())
                    .build();

            elasticsearchOperations.save(tourIndex);
            log.info("=> [Kafka Consumer] Đã đồng bộ trọn vẹn lên ES!");
        } catch (Exception e) {
            log.error("=> [Kafka Consumer] Lỗi: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "tour-search-topic", groupId = "search-group")
    public void consume(TourSearchEvent event) {
        log.info("Nhận dữ liệu tour từ Kafka: {}", event.getTourId());

        TourDocument doc = TourDocument.builder()
                .id(event.getTourId().toString())
                .title(event.getTitle())
                .destinations(event.getDestinations())
                .departures(event.getDepartures() == null ? java.util.List.of() : event.getDepartures().stream()
                        .map(d -> {
                            DepartureDoc dd = new DepartureDoc();
                            dd.setId(d.getId());
                            dd.setStartDate(d.getStartDate());
                            dd.setEndDate(d.getEndDate());
                            return dd;
                        }).toList())
                .build();

        elasticsearchOperations.save(doc);
    }
}