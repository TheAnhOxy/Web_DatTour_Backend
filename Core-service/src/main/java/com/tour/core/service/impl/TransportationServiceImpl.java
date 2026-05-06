package com.tour.core.service.impl;

import com.tour.core.dto.request.TransportationRequest;
import com.tour.core.dto.response.TransportationResponse;
import com.tour.core.entity.Transportation;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.TransportationRepository;
import com.tour.core.service.TransportationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransportationServiceImpl implements TransportationService {

    private final TransportationRepository transportationRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "transportations", key = "'all'")
    @Transactional(readOnly = true)
    public List<TransportationResponse> getAll() {
        return transportationRepository.findAll()
                .stream()
                .map(transportation -> modelMapper.map(transportation, TransportationResponse.class))
                .toList();
    }

    @Override
    @Cacheable(value = "transportations", key = "#id")
    @Transactional(readOnly = true)
    public TransportationResponse getById(Long id) {
        Transportation transportation = transportationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vận chuyển không tồn tại: " + id));
        return modelMapper.map(transportation, TransportationResponse.class);
    }

    @Override
    @CacheEvict(value = "transportations", allEntries = true)
    @Transactional
    public TransportationResponse create(TransportationRequest request) {
        if (transportationRepository.existsByType(request.getType())) {
            throw new InvalidDataException("Loại vận chuyển đã tồn tại");
        }

        Transportation transportation = modelMapper.map(request, Transportation.class);
        Transportation savedTransportation = transportationRepository.save(transportation);
        log.info("Created transportation - id={}, type={}, by={}", savedTransportation.getId(), savedTransportation.getType(), getCurrentUser());
        return modelMapper.map(savedTransportation, TransportationResponse.class);
    }

    @Override
    @CacheEvict(value = "transportations", allEntries = true)
    @Transactional
    public TransportationResponse update(Long id, TransportationRequest request) {
        Transportation transportation = transportationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vận chuyển không tồn tại: " + id));

        if (!transportation.getType().equals(request.getType()) && transportationRepository.existsByType(request.getType())) {
            throw new InvalidDataException("Loại vận chuyển đã tồn tại");
        }

        modelMapper.map(request, transportation);
        Transportation savedTransportation = transportationRepository.save(transportation);
        log.info("Updated transportation - id={}, type={}, by={}", savedTransportation.getId(), savedTransportation.getType(), getCurrentUser());
        return modelMapper.map(savedTransportation, TransportationResponse.class);
    }

    @Override
    @CacheEvict(value = "transportations", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Transportation transportation = transportationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vận chuyển không tồn tại: " + id));
        transportationRepository.delete(transportation);
        log.info("Deleted transportation - id={}, type={}, by={}", transportation.getId(), transportation.getType(), getCurrentUser());
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
