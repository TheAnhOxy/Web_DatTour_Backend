package com.tour.core.service.impl;

import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.dto.response.DepartureResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.dto.response.TourImageResponse;
import com.tour.core.entity.Destination;
import com.tour.core.entity.Tour;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DestinationRepository;
import com.tour.core.repository.TourImageRepository;
import com.tour.core.repository.TourRepository;
import com.tour.core.service.TourDestinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourDestinationServiceImpl implements TourDestinationService {

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final DestinationRepository destinationRepository;
    private final ModelMapper modelMapper;

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public TourDetailResponse addDestination(Long tourId, Long destinationId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + tourId));

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm đến không tồn tại: " + destinationId));

        if (tour.getDestinations() == null) {
            tour.setDestinations(new HashSet<>());
        }

        boolean alreadyExists = tour.getDestinations().stream()
                .anyMatch(d -> d.getId().equals(destinationId));
        if (alreadyExists) {
            throw new InvalidDataException("Điểm đến đã có trong tour này");
        }

        tour.getDestinations().add(destination);
        Tour savedTour = tourRepository.save(tour);
        log.info("Thêm destination id={} vào tour id={}", destinationId, tourId);
        return buildDetailResponse(savedTour);
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public TourDetailResponse removeDestination(Long tourId, Long destinationId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + tourId));

        if (tour.getDestinations() == null || tour.getDestinations().isEmpty()) {
            throw new ResourceNotFoundException("Tour này chưa có điểm đến nào");
        }

        boolean removed = tour.getDestinations().removeIf(d -> d.getId().equals(destinationId));
        if (!removed) {
            throw new ResourceNotFoundException("Điểm đến id=" + destinationId + " không có trong tour này");
        }

        Tour savedTour = tourRepository.save(tour);
        log.info("Xóa destination id={} khỏi tour id={}", destinationId, tourId);
        return buildDetailResponse(savedTour);
    }

    @Override
    @CacheEvict(value = "tours", allEntries = true)
    @Transactional
    public TourDetailResponse setDestinations(Long tourId, List<Long> destinationIds) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + tourId));

        if (destinationIds == null || destinationIds.isEmpty()) {
            tour.setDestinations(new HashSet<>());
            log.info("Xóa toàn bộ destinations của tour id={}", tourId);
        } else {
            List<Destination> destinations = destinationRepository.findAllById(destinationIds);
            if (destinations.size() != destinationIds.size()) {
                throw new ResourceNotFoundException("Một hoặc nhiều điểm đến không tồn tại");
            }
            tour.setDestinations(new HashSet<>(destinations));
            log.info("Gán {} destinations cho tour id={}", destinationIds.size(), tourId);
        }

        Tour savedTour = tourRepository.save(tour);
        return buildDetailResponse(savedTour);
    }

    private TourDetailResponse buildDetailResponse(Tour tour) {
        List<TourImageResponse> imageResponses = tourImageRepository.findByTourIdOrderBySortOrderAsc(tour.getId())
                .stream()
                .map(image -> modelMapper.map(image, TourImageResponse.class))
                .toList();

        List<DestinationResponse> destinationResponses = tour.getDestinations() == null ? List.of() :
                tour.getDestinations().stream()
                        .map(destination -> modelMapper.map(destination, DestinationResponse.class))
                        .toList();

        List<DepartureResponse> departureResponses = tour.getDepartures() == null ? List.of() :
                tour.getDepartures().stream()
                        .map(departure -> modelMapper.map(departure, DepartureResponse.class))
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
                .images(imageResponses)
                .destinations(destinationResponses)
                .departures(departureResponses)
                .build();
    }
}
