package com.tour.core.service.impl;

import com.tour.core.dto.request.PromotionRequest;
import com.tour.core.dto.response.PromotionResponse;
import com.tour.core.dto.response.PromotionValidateResponse;
import com.tour.core.entity.Promotion;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.PromotionRepository;
import com.tour.core.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "promotions", key = "'customer:' + (#isActive == null ? 'TRUE' : #isActive) + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getCustomerList(Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Boolean effectiveIsActive = (isActive == null) ? Boolean.TRUE : isActive;
        if (Boolean.FALSE.equals(effectiveIsActive)) {
            effectiveIsActive = Boolean.TRUE;
        }

        Page<Promotion> promotions = promotionRepository.findByIsActive(effectiveIsActive, pageable);

        return promotions.map(this::toResponse);
    }

    @Override
    @Cacheable(value = "promotions", key = "'staff:' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getStaffList(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return promotionRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion không tồn tại: " + id));
        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            throw new ResourceNotFoundException("Promotion không tồn tại: " + id);
        }
        return toResponse(promotion);
    }

    @Override
    @Cacheable(value = "promotions", key = "'validate:' + #code")
    @Transactional(readOnly = true)
    public PromotionValidateResponse validate(String code) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElse(null);

        if (promotion == null) {
            return PromotionValidateResponse.builder()
                    .code(code)
                    .isValid(false)
                    .message("Mã không tồn tại")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            return buildInvalidValidateResponse(promotion, "Mã không còn hiệu lực");
        }
        if (promotion.getValidFrom() != null && promotion.getValidTo() != null
                && (now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo()))) {
            return buildInvalidValidateResponse(promotion, "Mã đã hết hạn");
        }
        int usedCount = promotion.getUsedCount() == null ? 0 : promotion.getUsedCount();
        int usageLimit = promotion.getUsageLimit() == null ? 0 : promotion.getUsageLimit();
        if (usedCount >= usageLimit) {
            return buildInvalidValidateResponse(promotion, "Mã đã hết lượt dùng");
        }

        return PromotionValidateResponse.builder()
                .code(promotion.getCode())
                .isValid(true)
                .message("Mã hợp lệ")
                .discountPercent(promotion.getDiscountPercent())
                .maxDiscount(promotion.getMaxDiscount())
                .build();
    }

    @Override
    @CacheEvict(value = "promotions", allEntries = true)
    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        normalizeAndValidate(request, null);
        if (promotionRepository.existsByCode(request.getCode())) {
            throw new InvalidDataException("Mã promotion đã tồn tại");
        }

        Promotion promotion = modelMapper.map(request, Promotion.class);
        promotion.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        promotion.setUsedCount(0);
        promotion.setIsActive(true);

        Promotion saved = promotionRepository.save(promotion);
        log.info("User {} - create promotion - code={} id={}", getCurrentUser(), saved.getCode(), saved.getId());
        return toResponse(saved);
    }

    @Override
    @CacheEvict(value = "promotions", allEntries = true)
    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion không tồn tại: " + id));
        normalizeAndValidate(request, promotion);

        String oldCode = promotion.getCode();
        modelMapper.map(request, promotion);
        promotion.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        if (promotion.getUsedCount() == null) {
            promotion.setUsedCount(0);
        }

        Promotion saved = promotionRepository.save(promotion);
        log.info("User {} - update promotion - id={} oldCode={} newCode={}", getCurrentUser(), id, oldCode, saved.getCode());
        return toResponse(saved);
    }

    @Override
    @CacheEvict(value = "promotions", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion không tồn tại: " + id));
        if (promotion.getUsedCount() != null && promotion.getUsedCount() > 0) {
            throw new InvalidDataException("Không thể xóa promotion đã được sử dụng");
        }
        promotion.setIsActive(false);
        Promotion saved = promotionRepository.save(promotion);
        log.info("User {} - delete promotion(soft) - id={} code={} isActive={}", getCurrentUser(), id, saved.getCode(), saved.getIsActive());
    }

    @Override
    @CacheEvict(value = "promotions", allEntries = true)
    @Transactional
    public PromotionResponse toggle(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion không tồn tại: " + id));
        promotion.setIsActive(!Boolean.TRUE.equals(promotion.getIsActive()));
        Promotion saved = promotionRepository.save(promotion);
        log.info("User {} - toggle promotion - id={} isActive={}", getCurrentUser(), id, saved.getIsActive());
        return toResponse(saved);
    }

    private void normalizeAndValidate(PromotionRequest request, Promotion existing) {
        if (request.getValidFrom() != null && request.getValidTo() != null
                && !request.getValidFrom().isBefore(request.getValidTo())) {
            throw new InvalidDataException("validFrom phải nhỏ hơn validTo");
        }

        String code = request.getCode() == null ? null : request.getCode().trim();
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("Mã promotion không được để trống");
        }

        if (existing != null && !existing.getCode().equalsIgnoreCase(code) && promotionRepository.existsByCode(code)) {
            throw new InvalidDataException("Mã promotion đã tồn tại");
        }
    }

    private PromotionValidateResponse buildInvalidValidateResponse(Promotion promotion, String message) {
        return PromotionValidateResponse.builder()
                .code(promotion.getCode())
                .isValid(false)
                .message(message)
                .discountPercent(promotion.getDiscountPercent())
                .maxDiscount(promotion.getMaxDiscount())
                .build();
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return modelMapper.map(promotion, PromotionResponse.class);
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
