package com.tour.core.service.impl;

import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.request.DepartureRequest;
import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.dto.response.DepartureResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.dto.response.TourListResponse;
import com.tour.core.dto.response.TourImageResponse;
import com.tour.core.service.PriceConfigService;
import com.tour.core.entity.Destination;
import com.tour.core.entity.Tour;
import com.tour.core.entity.TourCategory;
import com.tour.core.entity.TourImage;
import com.tour.core.entity.Departure;
import com.tour.core.entity.Transportation;
import com.tour.core.event.DepartureEvent;
import com.tour.core.event.TourSearchEvent;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DestinationRepository;
import com.tour.core.repository.TourCategoryRepository;
import com.tour.core.repository.TourImageRepository;
import com.tour.core.repository.TourRepository;
import com.tour.core.repository.DepartureRepository;
import com.tour.core.repository.TransportationRepository;
import com.tour.core.service.TourService;
import com.tour.core.service.S3StorageService;
import com.tour.core.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final TourCategoryRepository tourCategoryRepository;
    private final TransportationRepository transportationRepository;
    private final DestinationRepository destinationRepository;
    private final DepartureRepository departureRepository;
    private final ModelMapper modelMapper;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final S3StorageService s3StorageService;
    private final PriceConfigService priceConfigService;

    // ── Queries ──

    @Override
    @Cacheable(value = "tours", key = "'customer:' + (#keyword == null ? 'ALL' : #keyword) + ':' + (#categoryId == null ? 'ALL' : #categoryId) + ':' + (#isHot == null ? 'ALL' : #isHot) + ':' + (#destinationId == null ? 'ALL' : #destinationId) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<TourListResponse> getAllForCustomer(String keyword, Long categoryId, Boolean isHot, Long destinationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return tourRepository.searchForAdmin(keyword, "ACTIVE", categoryId, isHot, destinationId, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Cacheable(value = "tours", key = "'admin:' + (#status == null ? 'ALL' : #status) + ':' + (#categoryId == null ? 'ALL' : #categoryId) + ':' + (#isHot == null ? 'ALL' : #isHot) + ':' + (#destinationId == null ? 'ALL' : #destinationId) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<TourListResponse> getAllForAdmin(String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return tourRepository.findByFilters(status, categoryId, isHot, destinationId, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourListResponse> searchForAdmin(String keyword, String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return tourRepository.searchForAdmin(keyword, status, categoryId, isHot, destinationId, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Cacheable(value = "tour_details", key = "#id")
    @Transactional(readOnly = true)
    public TourDetailResponse getById(Long id) {
        return toDetailResponse(findTourById(id));
    }

    @Override
    @Cacheable(value = "tour_details", key = "'slug:' + #slug")
    @Transactional(readOnly = true)
    public TourDetailResponse getBySlug(String slug) {
        Tour tour = tourRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại với slug: " + slug));
        return toDetailResponse(tour);
    }

    // ── Commands ──

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "tours", allEntries = true),
        @CacheEvict(value = "tour_details", allEntries = true)
    })
    @Transactional
    public TourDetailResponse create(TourRequest request, List<MultipartFile> images) {
        if (request.getDepartures() == null || request.getDepartures().isEmpty()) {
            throw new InvalidDataException("Phải có ít nhất một lịch khởi hành");
        }

        Tour savedTour = tourRepository.save(buildTourEntity(request));
        uploadAndSaveImages(savedTour, images);
        saveDepartures(savedTour, request.getDepartures());
        publishKafkaEvent(savedTour);

        log.info("Tạo tour: id={}, slug={} bởi {}", savedTour.getId(), savedTour.getSlug(), getCurrentUser());
        return toDetailResponse(savedTour);
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "tours", allEntries = true),
        @CacheEvict(value = "tour_details", allEntries = true)
    })
    @Transactional
    public TourDetailResponse update(Long id, TourRequest request) {
        Tour tour = findTourById(id);

        String nextSlug = SlugUtil.toSlug(request.getTitle());
        if (!nextSlug.equals(tour.getSlug()) && tourRepository.existsBySlug(nextSlug)) {
            nextSlug = nextSlug + "-" + System.currentTimeMillis();
        }

        TourCategory category = tourCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + request.getCategoryId()));
        Transportation transportation = transportationRepository.findById(request.getTransportationId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương tiện không tồn tại: " + request.getTransportationId()));

        modelMapper.map(request, tour);
        tour.setImages(null);
        tour.setSlug(nextSlug);
        tour.setCategory(category);
        tour.setTransportation(transportation);
        tour.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
        tour.setIsHot(Boolean.TRUE.equals(request.getIsHot()));

        Tour savedTour = tourRepository.save(tour);
        publishKafkaEvent(savedTour);

        if (request.getDepartures() != null) {
            if (request.getDepartures().isEmpty()) {
                throw new InvalidDataException("Cập nhật thất bại: nếu gửi danh sách departures thì phải có ít nhất một mục");
            }
            departureRepository.deleteByTourId(savedTour.getId());
            saveDepartures(savedTour, request.getDepartures());
        }

        log.info("Cập nhật tour: id={}, slug={} bởi {}", savedTour.getId(), savedTour.getSlug(), getCurrentUser());
        return toDetailResponse(savedTour);
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "tours", allEntries = true),
        @CacheEvict(value = "tour_details", allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        Tour tour = findTourById(id);
        tour.setStatus("INACTIVE");
        tourRepository.save(tour);
        log.info("Xóa mềm tour: id={}, slug={} bởi {}", tour.getId(), tour.getSlug(), getCurrentUser());
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "tours", allEntries = true),
        @CacheEvict(value = "tour_details", allEntries = true)
    })
    @Transactional
    public TourDetailResponse toggleHot(Long id) {
        Tour tour = findTourById(id);
        tour.setIsHot(!Boolean.TRUE.equals(tour.getIsHot()));
        Tour savedTour = tourRepository.save(tour);
        log.info("Đổi trạng thái hot tour id={} thành {} bởi {}", id, savedTour.getIsHot(), getCurrentUser());
        return toDetailResponse(savedTour);
    }

    // ── Private helpers ──

    private Tour buildTourEntity(TourRequest request) {
        String slug = buildUniqueSlug(request.getTitle());
        TourCategory category = tourCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + request.getCategoryId()));
        Transportation transportation = transportationRepository.findById(request.getTransportationId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương tiện không tồn tại: " + request.getTransportationId()));
        Set<Destination> destinations = resolveDestinations(request.getDestinationIds());

        Tour tour = modelMapper.map(request, Tour.class);
        tour.setImages(null);
        tour.setSlug(slug);
        tour.setCategory(category);
        tour.setTransportation(transportation);
        tour.setDestinations(destinations);
        tour.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
        tour.setIsHot(Boolean.TRUE.equals(request.getIsHot()));
        tour.setRating(BigDecimal.ZERO);
        tour.setReviewCount(0);
        return tour;
    }

    private void uploadAndSaveImages(Tour tour, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;

        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                if (file == null || file.isEmpty()) continue;

                String imageUrl = s3StorageService.upload(file, "tours/" + tour.getId());
                uploadedUrls.add(imageUrl);

                tourImageRepository.save(TourImage.builder()
                        .tour(tour).imageUrl(imageUrl).isCover(i == 0).sortOrder(i + 1).build());
            }
        } catch (RuntimeException ex) {
            uploadedUrls.forEach(url -> {
                try { s3StorageService.deleteByUrl(url); }
                catch (RuntimeException e) { log.warn("Không thể xóa ảnh S3 khi rollback: {}", url, e); }
            });
            throw new InvalidDataException("Upload ảnh thất bại, tour không được tạo: " + ex.getMessage());
        }
    }

    private void publishKafkaEvent(Tour tour) {
        TourSearchEvent event = TourSearchEvent.builder()
                .tourId(tour.getId()).title(tour.getTitle())
                .destinations(tour.getDestinations().stream().map(Destination::getCityName).toList())
                .departures(tour.getDepartures() == null ? List.of() : tour.getDepartures().stream()
                        .map(d -> DepartureEvent.builder().id(d.getId()).startDate(d.getStartDate()).endDate(d.getEndDate()).build())
                        .toList())
                .build();
        kafkaTemplate.send("tour-search-topic", event);
    }

    private void saveDepartures(Tour tour, List<DepartureRequest> departureRequests) {
        if (departureRequests == null || departureRequests.isEmpty()) return;

        for (DepartureRequest dr : departureRequests) {
            Departure d = modelMapper.map(dr, Departure.class);
            d.setTour(tour);
            d.setBookedSlots(0);
            Departure saved = departureRepository.save(d);
            redissonClient.getBucket("SLOTS_" + saved.getId()).set(saved.getMaxSlots());

            // Lưu priceConfig nếu có trong request
            if (dr.getPriceConfig() != null) {
                try {
                    priceConfigService.upsert(saved.getId(), dr.getPriceConfig());
                    log.info("Lưu priceConfig cho departure id={}", saved.getId());
                } catch (Exception e) {
                    log.warn("Không thể lưu priceConfig cho departure id={}: {}", saved.getId(), e.getMessage());
                }
            }
        }

        // Reload departures vào tour entity
        tour.setDepartures(departureRepository.findByTourId(tour.getId()));
    }

    private Tour findTourById(Long id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + id));
    }

    private String buildUniqueSlug(String title) {
        String baseSlug = SlugUtil.toSlug(title);
        if (baseSlug.isBlank()) throw new InvalidDataException("Không thể tạo slug từ tiêu đề");
        return tourRepository.existsBySlug(baseSlug) ? baseSlug + "-" + System.currentTimeMillis() : baseSlug;
    }

    private Set<Destination> resolveDestinations(List<Long> destinationIds) {
        if (destinationIds == null || destinationIds.isEmpty()) return Set.of();
        List<Destination> destinations = destinationRepository.findAllById(destinationIds);
        if (destinations.size() != destinationIds.size()) throw new ResourceNotFoundException("Một hoặc nhiều điểm đến không tồn tại");
        return destinations.stream().collect(Collectors.toSet());
    }

    // ── Mapping helpers ──

    private TourListResponse toListResponse(Tour tour) {
        Departure nextDeparture = findNextDeparture(tour);
        String coverImageUrl = tour.getImages() == null || tour.getImages().isEmpty() ? null :
                tour.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsCover())).findFirst()
                        .or(() -> tour.getImages().stream().findFirst())
                        .map(TourImage::getImageUrl).orElse(null);

        TourListResponse response = modelMapper.map(tour, TourListResponse.class);
        response.setCoverImageUrl(coverImageUrl);
        response.setCategoryName(tour.getCategory() != null ? tour.getCategory().getName() : null);
        response.setRegion(tour.getDestinations() != null && !tour.getDestinations().isEmpty()
                ? tour.getDestinations().iterator().next().getRegion() : null);
        response.setDepartureId(nextDeparture != null ? nextDeparture.getId() : null);
        response.setDepartureStartDate(nextDeparture != null ? nextDeparture.getStartDate() : null);
        response.setPickupName(nextDeparture != null ? nextDeparture.getPickupName() : null);
        response.setPickupAddress(nextDeparture != null ? nextDeparture.getPickupAddress() : null);
        response.setPickupTime(nextDeparture != null ? nextDeparture.getPickupTime() : null);
        response.setRating(tour.getRating());
        response.setReviewCount(tour.getReviewCount());
        return response;
    }

    private Departure findNextDeparture(Tour tour) {
        if (tour.getDepartures() == null || tour.getDepartures().isEmpty()) return null;
        return tour.getDepartures().stream()
                .filter(d -> d.getStartDate() != null)
                .min((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .orElse(tour.getDepartures().get(0));
    }

    private TourDetailResponse toDetailResponse(Tour tour) {
        List<TourImageResponse> imageResponses = tourImageRepository.findByTourIdOrderBySortOrderAsc(tour.getId())
                .stream().map(img -> modelMapper.map(img, TourImageResponse.class)).toList();
        List<DestinationResponse> destResponses = tour.getDestinations() == null ? List.of() :
                tour.getDestinations().stream().map(d -> modelMapper.map(d, DestinationResponse.class)).toList();
        List<DepartureResponse> depResponses = tour.getDepartures() == null ? List.of() :
                tour.getDepartures().stream().map(d -> modelMapper.map(d, DepartureResponse.class)).toList();

        TourDetailResponse response = modelMapper.map(tour, TourDetailResponse.class);
        response.setImages(imageResponses);
        response.setDestinations(destResponses);
        response.setDepartures(depResponses);
        response.setCategoryId(tour.getCategory() != null ? tour.getCategory().getId() : null);
        response.setCategoryName(tour.getCategory() != null ? tour.getCategory().getName() : null);
        response.setTransportationId(tour.getTransportation() != null ? tour.getTransportation().getId() : null);
        response.setTransportationType(tour.getTransportation() != null ? tour.getTransportation().getType() : null);
        return response;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
