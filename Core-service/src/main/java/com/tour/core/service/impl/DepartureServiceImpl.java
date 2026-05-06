package com.tour.core.service.impl;

import com.tour.core.dto.request.DepartureRequest;
import com.tour.core.dto.response.DepartureResponse;
import com.tour.core.dto.response.PriceConfigResponse;
import com.tour.core.entity.Departure;
import com.tour.core.entity.Tour;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DepartureRepository;
import com.tour.core.repository.TourRepository;
import com.tour.core.service.DepartureService;
import com.tour.core.service.PriceConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartureServiceImpl implements DepartureService {

    private static final String DEFAULT_STATUS = "OPEN";

    private final DepartureRepository departureRepository;
    private final TourRepository tourRepository;
    private final ModelMapper modelMapper;
    private final PriceConfigService priceConfigService;

    @Override
    @Cacheable(value = "departures", key = "'tour:' + #tourId")
    @Transactional(readOnly = true)
    public List<DepartureResponse> getByTourId(Long tourId) {
        List<Departure> deps = departureRepository.findByTourId(tourId);
        return deps.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "departures", key = "'tour:' + #tourId + ':open'")
    @Transactional(readOnly = true)
    public List<DepartureResponse> getOpenByTourId(Long tourId) {
        List<Departure> deps = departureRepository.findByTourIdAndStatus(tourId, "OPEN");
        return deps.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "departures", key = "'page:' + #tourId + ':' + (#status == null || #status.isBlank() ? 'ALL' : #status.trim().toUpperCase()) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<DepartureResponse> getByTourId(Long tourId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Departure> deps;
        if (status == null || status.isBlank()) {
            deps = departureRepository.findByTourId(tourId, pageable);
        } else {
            String normalizedStatus = normalizeStatus(status, null);
            deps = departureRepository.findByTourIdAndStatus(tourId, normalizedStatus, pageable);
        }
        return deps.map(this::toResponse);
    }

    @Override
    @Cacheable(value = "departures", key = "#id")
    @Transactional(readOnly = true)
    public DepartureResponse getById(Long id) {
        Departure d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + id));
        return toResponse(d);
    }

    @Override
    @CacheEvict(value = "departures", allEntries = true)
    @Transactional
    public DepartureResponse create(DepartureRequest request) {
        Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + request.getTourId()));
        validateDepartureRequest(request, null);

        Departure d = modelMapper.map(request, Departure.class);
        d.setTour(tour);
        d.setBookedSlots(0);
        d.setStatus(normalizeStatus(request.getStatus(), DEFAULT_STATUS));

        Departure saved = departureRepository.save(d);

        if (request.getPriceConfig() != null) {
            priceConfigService.upsert(saved.getId(), request.getPriceConfig());
        }

        log.info("Tạo departure: id={}, tourId={} bởi {}", saved.getId(), tour.getId(), getCurrentUser());
        return toResponse(saved);
    }

    @Override
    @CacheEvict(value = "departures", allEntries = true)
    @Transactional
    public DepartureResponse update(Long id, DepartureRequest request) {
        Departure d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + id));
        validateDepartureRequest(request, d);
        modelMapper.map(request, d);
        d.setStatus(normalizeStatus(request.getStatus(), d.getStatus()));

        Departure saved = departureRepository.save(d);

        if (request.getPriceConfig() != null) {
            priceConfigService.upsert(saved.getId(), request.getPriceConfig());
        }

        log.info("Cập nhật departure: id={} bởi {}", saved.getId(), getCurrentUser());
        return toResponse(saved);
    }

    @Override
    @CacheEvict(value = "departures", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Departure d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + id));
        if (d.getBookedSlots() != null && d.getBookedSlots() > 0) {
            throw new InvalidDataException("Không thể xóa departure đã có booking");
        }
        d.setStatus(normalizeStatus("CLOSED", d.getStatus()));
        departureRepository.save(d);
        log.info("Đánh dấu xóa mềm departure id={} -> CLOSED bởi {}", id, getCurrentUser());
    }

    @Override
    @CacheEvict(value = "departures", allEntries = true)
    @Transactional
    public DepartureResponse updateStatus(Long id, String status) {
        Departure d = departureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + id));
        d.setStatus(normalizeStatus(status, d.getStatus()));
        Departure saved = departureRepository.save(d);
        log.info("Cập nhật status departure id={} -> {} bởi {}", id, status, getCurrentUser());
        return toResponse(saved);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }

    private void validateDepartureRequest(DepartureRequest request, Departure existing) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new InvalidDataException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Integer currentBookedSlots = existing != null ? existing.getBookedSlots() : 0;
        if (request.getMaxSlots() != null && currentBookedSlots != null && request.getMaxSlots() < currentBookedSlots) {
            throw new InvalidDataException("Số chỗ tối đa không được nhỏ hơn số đã đặt");
        }

        normalizeStatus(request.getStatus(), null);
    }

    private String normalizeStatus(String status, String defaultValue) {
        if (status == null || status.isBlank()) {
            return defaultValue;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"OPEN".equals(normalized) && !"CLOSED".equals(normalized)) {
            throw new InvalidDataException("Trạng thái phải là OPEN hoặc CLOSED");
        }
        return normalized;
    }

    private DepartureResponse toResponse(Departure d) {
        DepartureResponse resp = modelMapper.map(d, DepartureResponse.class);
        resp.setTourId(d.getTour() != null ? d.getTour().getId() : null);
        resp.setTourTitle(d.getTour() != null ? d.getTour().getTitle() : null);

        PriceConfigResponse pc = priceConfigService.getByDepartureId(d.getId());
        resp.setPriceConfig(pc);
        return resp;
    }
}
