package com.tour.core.service.impl;

import com.tour.core.dto.request.PriceConfigRequest;
import com.tour.core.dto.response.PriceConfigResponse;
import com.tour.core.entity.PriceConfig;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DepartureRepository;
import com.tour.core.repository.PriceConfigRepository;
import com.tour.core.service.PriceConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceConfigServiceImpl implements PriceConfigService {

    private final PriceConfigRepository priceConfigRepository;
    private final DepartureRepository departureRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "priceConfigs", key = "#departureId")
    @Transactional(readOnly = true)
    public PriceConfigResponse getByDepartureId(Long departureId) {
        Optional<PriceConfig> pc = priceConfigRepository.findByDepartureId(departureId);
        return pc.map(p -> {
            PriceConfigResponse r = modelMapper.map(p, PriceConfigResponse.class);
            r.setDepartureId(p.getDeparture() != null ? p.getDeparture().getId() : null);
            return r;
        }).orElse(null);
    }

    @Override
    @CacheEvict(value = "priceConfigs", allEntries = true)
    @Transactional
    public PriceConfigResponse upsert(Long departureId, PriceConfigRequest request) {
        var dep = departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + departureId));
        validatePriceRequest(request);

        PriceConfig pc = priceConfigRepository.findByDepartureId(departureId).orElseGet(PriceConfig::new);
        modelMapper.map(request, pc);
        pc.setDeparture(dep);

        PriceConfig saved = priceConfigRepository.save(pc);
        log.info("Upsert price config for departureId={} by {}", departureId, getCurrentUser());

        PriceConfigResponse resp = modelMapper.map(saved, PriceConfigResponse.class);
        resp.setDepartureId(departureId);
        return resp;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }

    private void validatePriceRequest(PriceConfigRequest request) {
        if (isNegative(request.getAdultPrice())
                || isNegative(request.getChild1014Price())
                || isNegative(request.getChild49Price())
                || isNegative(request.getBabyPrice())) {
            throw new IllegalArgumentException("Giá phải lớn hơn hoặc bằng 0");
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }
}
