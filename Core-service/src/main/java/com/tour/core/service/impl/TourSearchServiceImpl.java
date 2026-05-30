package com.tour.core.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.tour.core.dto.request.TourSearchRequest;
import com.tour.core.dto.response.TourListResponse;
import com.tour.core.index.TourIndex;
import com.tour.core.service.TourSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourSearchServiceImpl implements TourSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public List<TourListResponse> searchTours(TourSearchRequest request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Khởi tạo Builder cho Bool Query
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Chỉ tìm kiếm các Tour đang có trạng thái ACTIVE
        boolQueryBuilder.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

        // 1. Tìm kiếm Full-text search theo keyword (Tên tour, điểm đến, địa danh, điểm đón)
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = request.getKeyword().trim();
            boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
                    .fields("title", "destinations", "categoryName", "pickupAddress")
                    .query(kw)
                    .fuzziness("AUTO") // Cho phép sai sót chính tả nhẹ (Ví dụ: Vũng Tàu -> VT)
            ));
        }

        // 2. Lọc theo khoảng giá (basePrice)
        if (request.getPriceFrom() != null || request.getPriceTo() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> {
                r.field("basePrice");
                if (request.getPriceFrom() != null) r.gte(JsonData.of(request.getPriceFrom()));
                if (request.getPriceTo() != null) r.lte(JsonData.of(request.getPriceTo()));
                return r;
            }));
        }

        // 3. Lọc theo khoảng ngày khởi hành (departureStartDate)
        if (request.getStartDateFrom() != null || request.getStartDateTo() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> {
                r.field("departureStartDate");
                if (request.getStartDateFrom() != null) {
                    r.gte(JsonData.of(request.getStartDateFrom().atStartOfDay().toString())); // 00:00:00
                }
                if (request.getStartDateTo() != null) {
                    r.lte(JsonData.of(request.getStartDateTo().atTime(LocalTime.MAX).toString())); // 23:59:59
                }
                return r;
            }));
        }

        // 4. Lọc theo thể loại Tour (Khớp chính xác từng từ)
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("categoryName.keyword").value(request.getCategoryName())));
        }

        // 5. Lọc theo phương tiện di chuyển
        if (request.getTransportationType() != null && !request.getTransportationType().isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("transportationType").value(request.getTransportationType())));
        }

        // 6. Lọc theo vùng miền
        if (request.getRegion() != null && !request.getRegion().isBlank()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("region").value(request.getRegion())));
        }

        // Đóng gói thành NativeQuery để thực thi
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(new Query(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        // Thực hiện truy vấn vào Elasticsearch
        SearchHits<TourIndex> searchHits = elasticsearchOperations.search(nativeQuery, TourIndex.class);

        // Chuyển đổi từ TourIndex (ES Document) sang chuẩn TourListResponse trả về cho FE
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
                        .pickupName(index.getPickupName())
                        .pickupAddress(index.getPickupAddress())
                        .rating(index.getRating())
                        .reviewCount(index.getReviewCount())
                        .transportationType(index.getTransportationType())
                        .build())
                .collect(Collectors.toList());
    }
}