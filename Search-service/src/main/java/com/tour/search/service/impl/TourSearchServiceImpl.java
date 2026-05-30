package com.tour.search.service.impl;


import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.tour.search.dto.request.TourSearchRequest;
import com.tour.search.dto.response.TourListResponse;
import com.tour.search.index.TourIndex;
import com.tour.search.service.TourSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourSearchServiceImpl implements TourSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    // ── 1. Logic tìm kiếm từ Elasticsearch phục vụ Controller ──
    @Override
    public List<TourListResponse> searchTours(TourSearchRequest request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.filter(f -> f.term(t -> t.field("status").value("active")));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = request.getKeyword().trim();
            boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
                    // 💡 SỬA 2: Thêm "destinations" vào đây để khách tìm theo địa danh (Bangkok, Đức) ra kết quả
                    .fields("title", "destinations", "categoryName", "pickupAddress")
                    .query(kw)
                    .fuzziness("AUTO")
            ));
        }

        if (request.getPriceFrom() != null || request.getPriceTo() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> {
                r.field("basePrice");
                if (request.getPriceFrom() != null) r.gte(JsonData.of(request.getPriceFrom()));
                if (request.getPriceTo() != null) r.lte(JsonData.of(request.getPriceTo()));
                return r;
            }));
        }

        if (request.getStartDateFrom() != null || request.getStartDateTo() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> {
                r.field("departureStartDate");
                if (request.getStartDateFrom() != null) r.gte(JsonData.of(request.getStartDateFrom().atStartOfDay().toString()));
                if (request.getStartDateTo() != null) r.lte(JsonData.of(request.getStartDateTo().atTime(LocalTime.MAX).toString()));
                return r;
            }));
        }

        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("categoryName.keyword").value(request.getCategoryName())));
        }

        if (request.getTransportationType() != null && !request.getTransportationType().isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("transportationType").value(request.getTransportationType())));
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(new Query(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<TourIndex> searchHits = elasticsearchOperations.search(nativeQuery, TourIndex.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(index -> TourListResponse.builder()
                        .id(Long.valueOf(index.getId()))
                        .title(index.getTitle())
                        .slug(index.getSlug())
                        .basePrice(index.getBasePrice())
                        .status(index.getStatus())
                        .isHot(index.getIsHot())
                        .coverImageUrl(index.getCoverImageUrl())
                        .categoryName(index.getCategoryName())
                        .durationDays(index.getDurationDays())
                        .region(index.getRegion())
                        .departureId(index.getDepartureId())
                        .departureStartDate(index.getDepartureStartDate())
                        .pickupTime(index.getPickupTime())
                        .pickupName(index.getPickupName())
                        .pickupAddress(index.getPickupAddress())
                        .rating(index.getRating())
                        .reviewCount(index.getReviewCount())
                        .transportationType(index.getTransportationType())
                        .build())
                .collect(Collectors.toList());
    }

    // ── 2. Logic lưu/cập nhật dữ liệu vào Elasticsearch phục vụ Kafka Consumer ──
    @Override
    public void saveOrUpdateIndex(TourListResponse tourData) {
        // Chuyển đổi dữ liệu từ DTO nhận từ Kafka sang cấu trúc Document của Elasticsearch
        TourIndex tourIndex = TourIndex.builder()
                .id(String.valueOf(tourData.getId())) // Ép ID về String làm Document ID trong ES
                .title(tourData.getTitle())
                .slug(tourData.getSlug())
                .basePrice(tourData.getBasePrice())
                .status(tourData.getStatus())
                .isHot(tourData.getIsHot())
                .coverImageUrl(tourData.getCoverImageUrl())
                .categoryName(tourData.getCategoryName())
                .durationDays(tourData.getDurationDays())
                .region(tourData.getRegion())
                .departureId(tourData.getDepartureId())
                .departureStartDate(tourData.getDepartureStartDate())
                .pickupName(tourData.getPickupName())
                .pickupAddress(tourData.getPickupAddress())
                .rating(tourData.getRating())
                .reviewCount(tourData.getReviewCount())
                .transportationType(tourData.getTransportationType())
                .build();

        // Thực hiện lưu trực tiếp bản ghi vào Elasticsearch
        elasticsearchOperations.save(tourIndex);
        log.info("=> Đã ghi thành công Tour ID {} vào Elasticsearch index!", tourIndex.getId());
    }
}
