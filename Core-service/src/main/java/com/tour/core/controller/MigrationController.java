//package com.tour.core.controller;
//
//import com.tour.core.entity.Tour;
//import com.tour.core.entity.Departure;
//import com.tour.core.entity.Destination;
//import com.tour.core.entity.TourImage;
//import com.tour.core.event.TourSearchEvent;
//import com.tour.core.repository.TourRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/core/migration")
//public class MigrationController {
//
//    @Autowired
//    private TourRepository tourRepository;
//
//    @Autowired
//    private KafkaTemplate<String, Object> kafkaTemplate;
//
//    @PostMapping("/sync-tours")
//    @Transactional(readOnly = true)
//    public ResponseEntity<String> syncAllToursToElasticsearch() {
//        List<Tour> allTours = tourRepository.findAll();
//
//        try {
//            for (Tour tour : allTours) {
//
//                // 1. LẤY ẢNH BÌA (Tìm tấm ảnh có isCover = true, nếu không có lấy tấm đầu tiên)
//                String coverUrl = null;
//                if (tour.getImages() != null && !tour.getImages().isEmpty()) {
//                    coverUrl = tour.getImages().stream()
//                            .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
//                            .map(TourImage::getImageUrl)
//                            .findFirst()
//                            .orElse(tour.getImages().get(0).getImageUrl());
//                }
//
//                // 2. LẤY THÔNG TIN KHỞI HÀNH & ĐIỂM ĐÓN (Bốc từ con Đột biến/Khởi hành đầu tiên)
//                Long depId = null;
//                java.time.LocalDateTime depStartDate = null;
//                String pName = null;
//                String pAddress = null;
//                java.time.LocalDateTime pTime = null;
//
//                if (tour.getDepartures() != null && !tour.getDepartures().isEmpty()) {
//                    Departure firstDep = tour.getDepartures().get(0);
//                    depId = firstDep.getId();
//                    depStartDate = firstDep.getStartDate();
//                    pName = firstDep.getPickupName();
//                    pAddress = firstDep.getPickupAddress();
//                    pTime = firstDep.getPickupTime();
//                }
//
//                // 3. LẤY THÔNG TIN VÙNG MIỀN & DANH SÁCH ĐIỂM ĐẾN
//                String tourRegion = null;
//                List<String> destList = null;
//
//                if (tour.getDestinations() != null && !tour.getDestinations().isEmpty()) {
//                    // Lấy region của điểm đến đầu tiên đại diện cho Tour
//                    Destination firstDest = tour.getDestinations().iterator().next();
//                    tourRegion = firstDest.getRegion();
//
//                    // Gom toàn bộ cityName của các Destination lại thành danh sách String
//                    destList = tour.getDestinations().stream()
//                            .map(Destination::getCityName)
//                            .collect(Collectors.toList());
//                }
//
//                // 4. MAPPING DỮ LIỆU SANG EVENT TRÙNG KHỚP LUỒNG TẠO MỚI
//                var event = TourSearchEvent.builder()
//                        .tourId(tour.getId())
//                        .title(tour.getTitle())
//                        .slug(tour.getSlug())
//                        .basePrice(tour.getBasePrice())
//                        .durationDays(tour.getDurationDays())
//                        .isHot(tour.getIsHot())
//                        .rating(tour.getRating())
//                        .reviewCount(tour.getReviewCount())
//
//                        // Các trường lấy từ bảng liên kết dữ liệu đã xử lý an toàn phía trên
//                        .coverImageUrl(coverUrl)
//                        .categoryName(tour.getCategory() != null ? tour.getCategory().getName() : null)
//                        .transportationType(tour.getTransportation() != null ? tour.getTransportation().getType() : null)
//
//                        .departureId(depId)
//                        .departureStartDate(depStartDate)
//                        .pickupName(pName)
//                        .pickupAddress(pAddress)
//                        .pickupTime(pTime)
//
//                        .region(tourRegion)
//                        .destinations(destList) // Gửi mảng danh sách thành phố sang đầu nhận
//                        .build();
//
//                // 5. BẮN TRỰC TIẾP VÀO ĐƯỜNG ỐNG ĐANG HOẠT ĐỘNG ỔN ĐỊNH
//                kafkaTemplate.send("tour-sync-topic", tour.getId().toString(), event);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Lỗi đồng bộ dữ liệu: " + e.getMessage());
//        }
//
//        return ResponseEntity.ok("Đã mồi thành công " + allTours.size() + " tours sang Kafka!");
//    }
//}