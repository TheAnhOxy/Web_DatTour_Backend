package com.tour.core.service.impl;

import com.tour.core.dto.request.DestinationRequest;
import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.entity.Destination;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DestinationRepository;
import com.tour.core.service.DestinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "destinations", key = "'all:' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<DestinationResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Destination> destinations = destinationRepository.findAll(pageable);
        return destinations.map(d -> modelMapper.map(d, DestinationResponse.class));
    }

    @Override
    @Cacheable(value = "destinations", key = "#id")
    @Transactional(readOnly = true)
    public DestinationResponse getById(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm đến không tồn tại: " + id));
        return modelMapper.map(destination, DestinationResponse.class);
    }

    @Override
    @Cacheable(value = "destinations", key = "'search:' + #keyword + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<DestinationResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Destination> destinations = destinationRepository
                .findByCityNameContainingIgnoreCaseOrRegionContainingIgnoreCaseOrCountryContainingIgnoreCase(
                        keyword, keyword, keyword, pageable);
        return destinations.map(d -> modelMapper.map(d, DestinationResponse.class));
    }

    @Override
    @CacheEvict(value = "destinations", allEntries = true)
    @Transactional
    public DestinationResponse create(DestinationRequest request) {
        Destination destination = modelMapper.map(request, Destination.class);
        Destination savedDestination = destinationRepository.save(destination);
        log.info("Created destination - id={}, cityName={}, country={}, by={}", 
            savedDestination.getId(), savedDestination.getCityName(), savedDestination.getCountry(), getCurrentUser());
        return modelMapper.map(savedDestination, DestinationResponse.class);
    }

    @Override
    @CacheEvict(value = "destinations", allEntries = true)
    @Transactional
    public DestinationResponse update(Long id, DestinationRequest request) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm đến không tồn tại: " + id));

        modelMapper.map(request, destination);
        Destination savedDestination = destinationRepository.save(destination);
        log.info("Updated destination - id={}, cityName={}, country={}, by={}", 
            savedDestination.getId(), savedDestination.getCityName(), savedDestination.getCountry(), getCurrentUser());
        return modelMapper.map(savedDestination, DestinationResponse.class);
    }

    @Override
    @CacheEvict(value = "destinations", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm đến không tồn tại: " + id));
        destinationRepository.delete(destination);
        log.info("Deleted destination - id={}, cityName={}, country={}, by={}", 
            destination.getId(), destination.getCityName(), destination.getCountry(), getCurrentUser());
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
