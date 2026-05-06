package com.tour.core.controller;

import com.tour.core.dto.request.WishlistRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.WishlistResponse;
import com.tour.core.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import com.tour.core.exception.ForBiddenException;
import com.tour.core.exception.InvalidDataException;

@RestController
@RequestMapping("/core/wishlists")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Quản lý danh sách yêu thích của người dùng")
@Validated
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse> getByUserId() {
        Long userId = getCurrentUserId();
        List<WishlistResponse> list = wishlistService.getByUserId(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy wishlist thành công")
                .data(list)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse> add(@Valid @RequestBody WishlistRequest request) {
        Long userId = getCurrentUserId();
        WishlistResponse resp = wishlistService.add(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.builder()
                        .status(201)
                        .message("Thêm wishlist thành công")
                        .data(resp)
                        .build());
    }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse> remove(@PathVariable("id") Long id) {
                Long userId = getCurrentUserId();
                wishlistService.remove(id, userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa wishlist thành công")
                .data(null)
                .build());
    }

        @GetMapping("/check")
        public ResponseEntity<ApiResponse> check(@RequestParam @NotNull @Positive Long tourId) {
                Long userId = getCurrentUserId();
                boolean exists = wishlistService.check(userId, tourId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Kiểm tra wishlist thành công")
                .data(exists)
                .build());
    }
    
        private Long getCurrentUserId() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null) throw new ForBiddenException("Chưa đăng nhập");
                Object principal = auth.getPrincipal();
                String userIdStr = null;
                if (principal instanceof Jwt) {
                        Jwt jwt = (Jwt) principal;
                        Object claim = jwt.getClaim("userId");
                        userIdStr = claim != null ? String.valueOf(claim) : jwt.getSubject();
                } else {
                        userIdStr = auth.getName();
                }
                try {
                        return Long.valueOf(userIdStr);
                } catch (Exception ex) {
                        throw new InvalidDataException("Không thể xác định userId từ token");
                }
        }
}
