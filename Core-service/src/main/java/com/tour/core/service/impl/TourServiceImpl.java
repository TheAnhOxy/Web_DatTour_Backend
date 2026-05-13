package com.tour.core.service.impl;

import com.tour.core.dto.request.TourImageRequest;
import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.request.DepartureRequest;
import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.dto.response.DepartureResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.dto.response.TourListResponse;
import com.tour.core.dto.response.TourImageResponse;
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
import com.tour.core.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.redisson.api.RBucket;
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

    @Override
    @Cacheable(value = "tours", key = "'customer:' + (#categoryId == null ? 'ALL' : #categoryId) + ':' + (#isHot == null ? 'ALL' : #isHot) + ':' + (#destinationId == null ? 'ALL' : #destinationId) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<TourListResponse> getAllForCustomer(Long categoryId, Boolean isHot, Long destinationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Tour> tours = tourRepository.findByFilters("ACTIVE", categoryId, isHot, destinationId, pageable);
        return tours.map(this::toListResponse);
    }

    @Override
    @Cacheable(value = "tours", key = "'admin:' + (#status == null ? 'ALL' : #status) + ':' + (#categoryId == null ? 'ALL' : #categoryId) + ':' + (#isHot == null ? 'ALL' : #isHot) + ':' + (#destinationId == null ? 'ALL' : #destinationId) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<TourListResponse> getAllForAdmin(String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Tour> tours = tourRepository.findByFilters(status, categoryId, isHot, destinationId, pageable);
        return tours.map(this::toListResponse);
    }

    @Override
    @Cacheable(value = "tours", key = "#id")
    @Transactional(readOnly = true)
    public TourDetailResponse getById(Long id) {
        Tour tour = findTourById(id);
        return toDetailResponse(tour);
    }

    @Override
    @Cacheable(value = "tours", key = "'slug:' + #slug")
    @Transactional(readOnly = true)
    public TourDetailResponse getBySlug(String slug) {
        Tour tour = tourRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại với slug: " + slug));
        return toDetailResponse(tour);
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public TourDetailResponse create(TourRequest request) {
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

        Tour savedTour = tourRepository.save(tour);

        TourSearchEvent event = TourSearchEvent.builder()
                .tourId(savedTour.getId())
                .title(savedTour.getTitle())
                .destinations(savedTour.getDestinations().stream().map(Destination::getCityName).toList())
                .departures(savedTour.getDepartures().stream()
                        .map(d -> DepartureEvent.builder().id(d.getId()).startDate(d.getStartDate()).endDate(d.getEndDate()).build())
                        .toList())
                .build();
        kafkaTemplate.send("tour-search-topic", event);

        saveTourImages(savedTour, request.getImages());
        
        // kiểm tra nếu có departures, nếu không có thì throw lỗi vì tour phải có ít nhất 1 lịch khởi hành để khách đặt
        if (request.getDepartures() == null || request.getDepartures().isEmpty()) {
            throw new InvalidDataException("Phải có ít nhất một lịch khởi hành");
        }
        saveDepartures(savedTour, request.getDepartures());

        log.info("Tạo tour: id={}, slug={} bởi {}", savedTour.getId(), slug, getCurrentUser());

        return toDetailResponse(savedTour);
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
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

        Set<Destination> destinations = resolveDestinations(request.getDestinationIds());

        modelMapper.map(request, tour);
        tour.setImages(null);
        tour.setSlug(nextSlug);
        tour.setCategory(category);
        tour.setTransportation(transportation);
        tour.setDestinations(destinations);
        tour.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "ACTIVE" : request.getStatus());
        tour.setIsHot(Boolean.TRUE.equals(request.getIsHot()));

        Tour savedTour = tourRepository.save(tour);
        // BẮN KAFKA ĐỂ SEARCH SERVICE CẬP NHẬT:
        TourSearchEvent event = TourSearchEvent.builder()
                .tourId(savedTour.getId())
                .title(savedTour.getTitle())
                .destinations(savedTour.getDestinations().stream()
                        .map(Destination::getCityName) // Lấy tên thành phố từ ID
                        .toList())
                .departures(savedTour.getDepartures().stream()
                        .map(d -> DepartureEvent.builder()
                                .id(d.getId())
                                .startDate(d.getStartDate())
                                .endDate(d.getEndDate())
                                .build())
                        .toList())
                .build();
        kafkaTemplate.send("tour-search-topic", event);

        tourImageRepository.deleteByTourId(savedTour.getId());
        saveTourImages(savedTour, request.getImages());

        // nếu gửi danh sách departures thì phải có ít nhất một mục, nếu không gửi thì giữ nguyên lịch khởi hành cũ
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

    private void saveDepartures(Tour tour, List<DepartureRequest> departureRequests) {
        if (departureRequests == null || departureRequests.isEmpty()) return;

        List<Departure> departures = departureRequests.stream()
                .map(dr -> {
                    Departure d = modelMapper.map(dr, Departure.class);
                    d.setTour(tour);
                    d.setBookedSlots(0);
                    return d;
                }).collect(Collectors.toList());

        List<Departure> savedDeps = departureRepository.saveAll(departures);
        tour.setDepartures(savedDeps);
        savedDeps.forEach(sd -> {
            redissonClient.getBucket("SLOTS_" + sd.getId()).set(sd.getMaxSlots());
        });
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Tour tour = findTourById(id);
        tour.setStatus("INACTIVE");
        tourRepository.save(tour);
        log.info("Xóa mềm tour: id={}, slug={} bởi {}", tour.getId(), tour.getSlug(), getCurrentUser());
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public TourDetailResponse toggleHot(Long id) {
        Tour tour = findTourById(id);
        tour.setIsHot(!Boolean.TRUE.equals(tour.getIsHot()));
        Tour savedTour = tourRepository.save(tour);
        log.info("Đổi trạng thái hot tour id={} thành {} bởi {}", id, savedTour.getIsHot(), getCurrentUser());
        return toDetailResponse(savedTour);
    }

    private Tour findTourById(Long id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + id));
    }

    private String buildUniqueSlug(String title) {
        String baseSlug = SlugUtil.toSlug(title);
        if (baseSlug.isBlank()) {
            throw new InvalidDataException("Không thể tạo slug từ tiêu đề");
        }

        if (tourRepository.existsBySlug(baseSlug)) {
            return baseSlug + "-" + System.currentTimeMillis();
        }
        return baseSlug;
    }

    private Set<Destination> resolveDestinations(List<Long> destinationIds) {
        if (destinationIds == null || destinationIds.isEmpty()) {
            return Set.of();
        }

        List<Destination> destinations = destinationRepository.findAllById(destinationIds);
        if (destinations.size() != destinationIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều điểm đến không tồn tại");
        }

        return destinations.stream().collect(Collectors.toSet());
    }

    private void saveTourImages(Tour tour, List<TourImageRequest> imageRequests) {
        if (imageRequests == null || imageRequests.isEmpty()) {
            return;
        }

        List<TourImage> images = new ArrayList<>();
        for (TourImageRequest request : imageRequests) {
            TourImage image = TourImage.builder()
                    .tour(tour)
                    .imageUrl(request.getImageUrl())
                    .isCover(Boolean.TRUE.equals(request.getIsCover()))
                    .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                    .build();
            images.add(image);
        }

        tourImageRepository.saveAll(images);
    }

    private TourListResponse toListResponse(Tour tour) {
        Departure nextDeparture = findNextDeparture(tour);

        String coverImageUrl = null;
        if (tour.getImages() != null && !tour.getImages().isEmpty()) {
            coverImageUrl = tour.getImages().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getIsCover()))
                    .findFirst()
                    .or(() -> tour.getImages().stream().findFirst())
                    .map(TourImage::getImageUrl)
                    .orElse(null);
        }

        return TourListResponse.builder()
                .id(tour.getId())
                .title(tour.getTitle())
                .slug(tour.getSlug())
                .basePrice(tour.getBasePrice())
                .status(tour.getStatus())
                .isHot(tour.getIsHot())
                .coverImageUrl(coverImageUrl)
                .categoryName(tour.getCategory() != null ? tour.getCategory().getName() : null)
                .durationDays(tour.getDurationDays())
                .region(tour.getDestinations() != null && !tour.getDestinations().isEmpty() 
                        ? tour.getDestinations().iterator().next().getRegion() : null)
                .departureId(nextDeparture != null ? nextDeparture.getId() : null)
                .departureStartDate(nextDeparture != null ? nextDeparture.getStartDate() : null)
                .pickupName(nextDeparture != null ? nextDeparture.getPickupName() : null)
                .pickupAddress(nextDeparture != null ? nextDeparture.getPickupAddress() : null)
                .pickupTime(nextDeparture != null ? nextDeparture.getPickupTime() : null)
                .build();
    }

    private Departure findNextDeparture(Tour tour) {
        if (tour.getDepartures() == null || tour.getDepartures().isEmpty()) {
            return null;
        }

        return tour.getDepartures().stream()
                .filter(departure -> departure.getStartDate() != null)
                .min((left, right) -> left.getStartDate().compareTo(right.getStartDate()))
                .orElse(tour.getDepartures().get(0));
    }

    private TourDetailResponse toDetailResponse(Tour tour) {
        List<TourImageResponse> imageResponses = tourImageRepository.findByTourIdOrderBySortOrderAsc(tour.getId())
                .stream()
                .map(image -> modelMapper.map(image, TourImageResponse.class))
                .toList();

        List<DestinationResponse> destinationResponses = tour.getDestinations() == null ? List.of() :
                tour.getDestinations().stream()
                        .map(destination -> modelMapper.map(destination, DestinationResponse.class))
                        .toList();

        return TourDetailResponse.builder()
                .id(tour.getId())
                .title(tour.getTitle())
                .slug(tour.getSlug())
                .description(tour.getDescription())
                .durationDays(tour.getDurationDays())
                .status(tour.getStatus())
                .isHot(tour.getIsHot())
                .basePrice(tour.getBasePrice())
                .categoryId(tour.getCategory() != null ? tour.getCategory().getId() : null)
                .categoryName(tour.getCategory() != null ? tour.getCategory().getName() : null)
                .transportationId(tour.getTransportation() != null ? tour.getTransportation().getId() : null)
                .transportationType(tour.getTransportation() != null ? tour.getTransportation().getType() : null)
                .createdAt(tour.getCreatedAt())
                .overview(tour.getOverview())
                .itinerary(tour.getItinerary())
                .inclusions(tour.getInclusions())
                .exclusions(tour.getExclusions())
                .policies(tour.getPolicies())
                .rating(tour.getRating())
                .reviewCount(tour.getReviewCount())
                .images(imageResponses)
                .destinations(destinationResponses)
                .departures(tour.getDepartures() == null ? List.of() : tour.getDepartures().stream()
                    .map(departure -> modelMapper.map(departure, DepartureResponse.class))
                    .toList())
                .build();
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
