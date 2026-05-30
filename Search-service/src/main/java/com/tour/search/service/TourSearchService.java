package com.tour.search.service;

import com.tour.search.dto.request.TourSearchRequest;
import com.tour.search.dto.response.TourListResponse;
import java.util.List;

public interface TourSearchService {
    // Hàm phục vụ FE tìm kiếm lọc nâng cao
    List<TourListResponse> searchTours(TourSearchRequest request, int page, int size);

    // Hàm phục vụ Kafka Consumer gọi để nạp/cập nhật dữ liệu vào ES
    void saveOrUpdateIndex(TourListResponse tourData);
}