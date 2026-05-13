package com.tour.core.service.impl;

import com.tour.core.dto.request.DestinationRequest;
import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.entity.Destination;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DestinationRepository;
import com.tour.core.service.DestinationService;
import com.tour.core.service.S3StorageService;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;
    private final ModelMapper modelMapper;
    private final S3StorageService s3StorageService;

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
    public DestinationResponse create(DestinationRequest request, MultipartFile file) {
        String uploadedImageUrl = null;
        if (file != null && !file.isEmpty()) {
            uploadedImageUrl = s3StorageService.upload(file, "destinations");
            request.setImageUrl(uploadedImageUrl);
        }

        Destination destination = applyRequest(new Destination(), request);
        Destination savedDestination;
        try {
            savedDestination = destinationRepository.save(destination);
        } catch (RuntimeException ex) {
            if (uploadedImageUrl != null && !uploadedImageUrl.isBlank()) {
                s3StorageService.deleteByUrl(uploadedImageUrl);
            }
            throw ex;
        }

        log.info("Created destination - id={}, cityName={}, country={}, by={}", 
            savedDestination.getId(), savedDestination.getCityName(), savedDestination.getCountry(), getCurrentUser());
        return modelMapper.map(savedDestination, DestinationResponse.class);
    }

    @Override
    @CacheEvict(value = "destinations", allEntries = true)
    @Transactional
    public DestinationResponse update(Long id, DestinationRequest request, MultipartFile file) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm đến không tồn tại: " + id));

        String previousImageUrl = destination.getImageUrl();
        String uploadedImageUrl = null;
        if (file != null && !file.isEmpty()) {
            uploadedImageUrl = s3StorageService.upload(file, "destinations");
            request.setImageUrl(uploadedImageUrl);
        }

        String nextImageUrl = request.getImageUrl();
        applyRequest(destination, request);

        Destination savedDestination;
        try {
            savedDestination = destinationRepository.save(destination);
        } catch (RuntimeException ex) {
            if (uploadedImageUrl != null && !uploadedImageUrl.isBlank()) {
                s3StorageService.deleteByUrl(uploadedImageUrl);
            }
            throw ex;
        }

        if (nextImageUrl != null
            && previousImageUrl != null && !previousImageUrl.isBlank()
            && !previousImageUrl.equals(nextImageUrl)) {
            s3StorageService.deleteByUrl(previousImageUrl);
        }

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

    private Destination applyRequest(Destination destination, DestinationRequest request) {
        destination.setCityName(request.getCityName());
        destination.setRegion(request.getRegion());
        destination.setCountry(request.getCountry());
        if (request.getImageUrl() != null) {
            destination.setImageUrl(request.getImageUrl());
        }
        return destination;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
