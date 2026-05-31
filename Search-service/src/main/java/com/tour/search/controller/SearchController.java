package com.tour.search.controller;

import co.elastic.clients.json.JsonData;
import com.tour.search.document.TourDocument;
import com.tour.search.dto.request.TourSearchRequest;
import com.tour.search.dto.response.ApiResponse;
import com.tour.search.dto.response.TourListResponse;
import com.tour.search.service.TourSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; // THÊM DÒNG NÀY
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final TourSearchService tourSearchService;
    private final ElasticsearchOperations elasticsearchOperations;
    private static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @PostMapping
    public ApiResponse search(
            @RequestBody TourSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("=> [API Search] Tiếp nhận yêu cầu tìm kiếm: keyword='{}', priceTo='{}'",
                request.getKeyword(), request.getPriceTo());

        List<TourListResponse> result = tourSearchService.searchTours(request, page, size);

        // Đóng gói theo chuẩn ApiResponse của hệ thống tiền bối
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm dữ liệu từ Elasticsearch thành công")
                .data(result)
                .build();
    }
    @GetMapping("/tours")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    // 1. Tìm theo điểm đến (Mức Tour)
                    if (destination != null && !destination.isBlank()) {
                        b.must(m -> m.match(mt -> mt.field("destinations").query(destination)));
                    }

                    // Trong Query Builder
                    if (startDate != null || endDate != null) {
                        b.must(m -> m.nested(n -> n
                                .path("departures")
                                .query(nq -> nq.bool(nb -> {
                                    // Chỉ cần kiểm tra từng trường, không cần bọc thêm 1 tầng if(startDate != null || endDate != null) nữa
                                    if (startDate != null) {
                                        String startStr = startDate.atStartOfDay().format(ES_DATE_FORMATTER);
                                        nb.must(nm -> nm.range(r -> r.field("departures.startDate").gte(JsonData.of(startStr))));
                                    }
                                    if (endDate != null) {
                                        String endStr = endDate.atTime(LocalTime.MAX).format(ES_DATE_FORMATTER);
                                        nb.must(nm -> nm.range(r -> r.field("departures.endDate").lte(JsonData.of(endStr))));
                                    }
                                    return nb;
                                }))
                        ));
                    }
                    return b;
                }))
                .build();

        SearchHits<TourDocument> hits = elasticsearchOperations.search(query, TourDocument.class);
        return ResponseEntity.ok(hits.getSearchHits().stream().map(hit -> hit.getContent()).toList());
    }
}