# Booking Service - Code Templates & Quick Reference

Tài liệu này cung cấp code templates sẵn sàng copy-paste để chuẩn bị implementation.

---

## 1. DTOs Template

### File: `BookingDetailResponse.java`
```java
package com.tour.booking.dto.response;

import com.tour.booking.dto.PassengerDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {
    
    @JsonProperty("bookingId")
    private Long bookingId;
    
    @JsonProperty("bookingCode")
    private String bookingCode;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("departureId")
    private Long departureId;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("paidAmount")
    private BigDecimal paidAmount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("paymentDueAt")
    private LocalDateTime paymentDueAt;
    
    @JsonProperty("contactName")
    private String contactName;
    
    @JsonProperty("contactEmail")
    private String contactEmail;
    
    @JsonProperty("contactPhone")
    private String contactPhone;
    
    @JsonProperty("priceSnapshot")
    private Map<String, Object> priceSnapshot;
    
    @JsonProperty("promotionSnapshot")
    private Map<String, Object> promotionSnapshot;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("version")
    private Long version;
    
    @JsonProperty("passengers")
    private List<PassengerDTO> passengers;
    
    @JsonProperty("bookingNotes")
    private List<BookingNoteDTO> bookingNotes;
    
    @JsonProperty("cancellation")
    private CancellationDTO cancellation;
}
```

### File: `BookingNoteDTO.java`
```java
package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingNoteDTO {
    
    @JsonProperty("noteId")
    private Long noteId;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
```

### File: `CancellationDTO.java`
```java
package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDTO {
    
    @JsonProperty("cancellationId")
    private Long cancellationId;
    
    @JsonProperty("bookingId")
    private Long bookingId;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("refundAmount")
    private BigDecimal refundAmount;
    
    @JsonProperty("cancelledAt")
    private LocalDateTime cancelledAt;
}
```

### File: `BookingSummaryDTO.java`
```java
package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDTO {
    
    @JsonProperty("totalBookings")
    private Integer totalBookings;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("totalPaidAmount")
    private BigDecimal totalPaidAmount;
    
    @JsonProperty("byStatus")
    private Map<String, Integer> byStatus;
}
```

### File: `PaginatedResponse.java`
```java
package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("totalElements")
    private Integer totalElements;
    
    @JsonProperty("currentPage")
    private Integer currentPage;
    
    @JsonProperty("pageSize")
    private Integer pageSize;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("hasNext")
    private Boolean hasNext;
}
```

### File: `BatchBookingRequest.java`
```java
package com.tour.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchBookingRequest {
    
    @JsonProperty("bookingIds")
    private List<Long> bookingIds;
}
```

---

## 2. Repository Templates

### File: Update `BookingRepository.java`
```java
package com.tour.booking.repository;

import com.tour.booking.entity.Booking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, 
                                          JpaSpecificationExecutor<Booking> {
    
    // Existing methods...
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // ===== NEW GET API METHODS =====
    
    // 1. Find by Code with all relationships
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.bookingCode = :bookingCode")
    Optional<Booking> findByBookingCodeFetchAll(@Param("bookingCode") String bookingCode);
    
    // 2. Find by ID with all relationships
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdFetchAll(@Param("id") Long id);
    
    // 3. Find by User ID with passengers (paginated)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.userId = :userId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithPassengers(
            @Param("userId") Long userId, 
            Pageable pageable);
    
    // 4. Find by User ID and Status
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.userId = :userId AND b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdAndStatusWithPassengers(
            @Param("userId") Long userId, 
            @Param("status") String status, 
            Pageable pageable);
    
    // 5. Find all with passengers (paginated)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithPassengers(Pageable pageable);
    
    // 6. Find by status with passengers
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByStatusWithPassengers(
            @Param("status") String status, 
            Pageable pageable);
    
    // 7. Find by IDs with all relationships (batch)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.id IN :ids")
    List<Booking> findByIdInFetchAll(@Param("ids") List<Long> ids);
    
    // 8. Count by User ID
    Long countByUserId(Long userId);
    
    // 9. Count by User ID and Status
    Long countByUserIdAndStatus(Long userId, String status);
    
    // 10. Existing cleanup methods...
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND (b.paymentMethod IS NULL OR b.paymentMethod <> 'CASH_OFFICE') AND b.createdAt < :cutoff")
    List<Booking> findOnlinePendingExpired(@Param("cutoff") LocalDateTime cutoff);

    List<Booking> findByStatusAndPaymentMethodAndPaymentDueAtBefore(
            String status, String paymentMethod, LocalDateTime paymentDueAt);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers")
    List<Booking> findAllWithPassengers();

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithPassengers(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.userId IN :userIds")
    List<Booking> findAllByUserIdsWithPassengers(@Param("userIds") List<Long> userIds);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.id IN :ids")
    List<Booking> findAllByIdsWithPassengers(@Param("ids") List<Long> ids);
}
```

### File: `PassengerRepository.java`
```java
package com.tour.booking.repository;

import com.tour.booking.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    // Find all passengers by booking
    List<Passenger> findByBookingId(Long bookingId);
    
    // Find by booking code
    @Query(value = "SELECT p.* FROM passenger p " +
                   "JOIN bookings b ON p.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode", 
           nativeQuery = true)
    List<Passenger> findByBookingCode(@Param("bookingCode") String bookingCode);
}
```

### File: `BookingNoteRepository.java`
```java
package com.tour.booking.repository;

import com.tour.booking.entity.BookingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingNoteRepository extends JpaRepository<BookingNote, Long> {
    
    // Find all notes by booking, sorted by creation time
    List<BookingNote> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    
    // Find by booking code
    @Query(value = "SELECT bn.* FROM booking_notes bn " +
                   "JOIN bookings b ON bn.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode " +
                   "ORDER BY bn.created_at DESC", 
           nativeQuery = true)
    List<BookingNote> findByBookingCode(@Param("bookingCode") String bookingCode);
}
```

### File: `CancellationRepository.java`
```java
package com.tour.booking.repository;

import com.tour.booking.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {
    
    // Find by booking ID (1:1 relationship)
    Optional<Cancellation> findByBookingId(Long bookingId);
    
    // Find by booking code
    @Query(value = "SELECT c.* FROM cancellations c " +
                   "JOIN bookings b ON c.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode", 
           nativeQuery = true)
    Optional<Cancellation> findByBookingCode(@Param("bookingCode") String bookingCode);
}
```

---

## 3. Service Interface & Implementation Templates

### File: Update `BookingService.java` Interface
```java
package com.tour.booking.service;

import com.tour.booking.dto.PassengerDTO;
import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.*;

import java.util.List;
import java.util.Map;

public interface BookingService {
    
    // ===== GET OPERATIONS =====
    BookingDetailResponse getBookingByCode(String bookingCode);
    BookingDetailResponse getBookingById(Long bookingId);
    
    PaginatedResponse<BookingResponse> getBookingsByUserId(
            Long userId, 
            String status, 
            Integer page, 
            Integer limit);
    
    PaginatedResponse<BookingResponse> getAllBookings(
            String status, 
            String paymentMethod, 
            Integer page, 
            Integer limit);
    
    List<PassengerDTO> getPassengersByBookingCode(String bookingCode);
    List<BookingNoteDTO> getBookingNotesByBookingCode(String bookingCode);
    CancellationDTO getCancellationByBookingCode(String bookingCode);
    
    Map<Long, BookingDetailResponse> getBookingsByIds(List<Long> bookingIds);
    BookingSummaryDTO getUserBookingSummary(Long userId);
    
    // ===== EXISTING METHODS =====
    BookingResponse createBooking(BookingRequest request);
    void cancelBooking(CancelBookingRequest request);
    
    // ===== HELPER METHODS (optional) =====
    List<BookingResponse> getBookingsByUserIds(List<Long> userIds);
    Map<Long, BookingResponse> getBookingsByIdsMapped(List<Long> ids);
}
```

### File: Add Methods to `BookingServiceImpl.java`

```java
// Add these imports to your BookingServiceImpl
import com.tour.booking.repository.BookingNoteRepository;
import com.tour.booking.repository.CancellationRepository;
import com.tour.booking.repository.PassengerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.stream.Collectors;

// ===== Add these methods to BookingServiceImpl class =====

@Override
@Transactional(readOnly = true)
public BookingDetailResponse getBookingByCode(String bookingCode) {
    log.info("Getting booking by code: {}", bookingCode);
    
    Booking booking = bookingRepository.findByBookingCodeFetchAll(bookingCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy đơn hàng với mã: " + bookingCode));
    
    return mapToDetailResponse(booking);
}

@Override
@Transactional(readOnly = true)
public BookingDetailResponse getBookingById(Long bookingId) {
    log.info("Getting booking by id: {}", bookingId);
    
    Booking booking = bookingRepository.findByIdFetchAll(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy đơn hàng với ID: " + bookingId));
    
    return mapToDetailResponse(booking);
}

@Override
@Transactional(readOnly = true)
public PaginatedResponse<BookingResponse> getBookingsByUserId(
        Long userId, 
        String status, 
        Integer page, 
        Integer limit) {
    
    log.info("Getting bookings for user: {} with status: {}", userId, status);
    
    Pageable pageable = PageRequest.of(page, limit, 
            Sort.by("createdAt").descending());
    
    List<Booking> bookings;
    Long total;
    
    if (status != null && !status.isEmpty()) {
        bookings = bookingRepository.findByUserIdAndStatusWithPassengers(
                userId, status, pageable);
        total = bookingRepository.countByUserIdAndStatus(userId, status);
    } else {
        bookings = bookingRepository.findByUserIdWithPassengers(userId, pageable);
        total = bookingRepository.countByUserId(userId);
    }
    
    List<BookingResponse> responses = bookings.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    
    int totalPages = (int) Math.ceil((double) total / limit);
    
    return PaginatedResponse.<BookingResponse>builder()
            .data(responses)
            .totalElements(total.intValue())
            .currentPage(page)
            .pageSize(limit)
            .totalPages(totalPages)
            .hasNext((page + 1) * limit < total)
            .build();
}

@Override
@Transactional(readOnly = true)
public PaginatedResponse<BookingResponse> getAllBookings(
        String status, 
        String paymentMethod, 
        Integer page, 
        Integer limit) {
    
    log.info("Getting all bookings with status: {}, paymentMethod: {}", status, paymentMethod);
    
    Pageable pageable = PageRequest.of(page, limit, 
            Sort.by("createdAt").descending());
    
    // Build specification for dynamic filtering
    Specification<Booking> spec = Specification.where(null);
    
    if (status != null && !status.isEmpty()) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    
    if (paymentMethod != null && !paymentMethod.isEmpty()) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentMethod"), paymentMethod));
    }
    
    var pageResult = bookingRepository.findAll(spec, pageable);
    
    List<BookingResponse> responses = pageResult.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    
    return PaginatedResponse.<BookingResponse>builder()
            .data(responses)
            .totalElements((int) pageResult.getTotalElements())
            .currentPage(page)
            .pageSize(limit)
            .totalPages(pageResult.getTotalPages())
            .hasNext(pageResult.hasNext())
            .build();
}

@Override
@Transactional(readOnly = true)
public List<PassengerDTO> getPassengersByBookingCode(String bookingCode) {
    log.info("Getting passengers for booking: {}", bookingCode);
    
    List<Passenger> passengers = passengerRepository.findByBookingCode(bookingCode);
    
    if (passengers.isEmpty()) {
        throw new ResourceNotFoundException(
                "Không tìm thấy hành khách cho đơn hàng: " + bookingCode);
    }
    
    return passengers.stream()
            .map(this::mapToPassengerDTO)
            .collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public List<BookingNoteDTO> getBookingNotesByBookingCode(String bookingCode) {
    log.info("Getting notes for booking: {}", bookingCode);
    
    List<BookingNote> notes = bookingNoteRepository.findByBookingCode(bookingCode);
    
    return notes.stream()
            .map(this::mapToBookingNoteDTO)
            .collect(Collectors.toList());
}

@Override
@Transactional(readOnly = true)
public CancellationDTO getCancellationByBookingCode(String bookingCode) {
    log.info("Getting cancellation info for booking: {}", bookingCode);
    
    var cancellation = cancellationRepository.findByBookingCode(bookingCode);
    
    if (cancellation.isEmpty()) {
        return null; // No cancellation
    }
    
    return mapToCancellationDTO(cancellation.get());
}

@Override
@Transactional(readOnly = true)
public Map<Long, BookingDetailResponse> getBookingsByIds(List<Long> bookingIds) {
    log.info("Getting bookings by ids: {}", bookingIds);
    
    List<Booking> bookings = bookingRepository.findByIdInFetchAll(bookingIds);
    
    return bookings.stream()
            .collect(Collectors.toMap(
                    Booking::getId,
                    this::mapToDetailResponse
            ));
}

@Override
@Transactional(readOnly = true)
public BookingSummaryDTO getUserBookingSummary(Long userId) {
    log.info("Getting booking summary for user: {}", userId);
    
    List<Booking> userBookings = bookingRepository
            .findByUserIdOrderByCreatedAtDesc(userId);
    
    if (userBookings.isEmpty()) {
        return BookingSummaryDTO.builder()
                .totalBookings(0)
                .totalAmount(BigDecimal.ZERO)
                .totalPaidAmount(BigDecimal.ZERO)
                .byStatus(new HashMap<>())
                .build();
    }
    
    BigDecimal totalAmount = userBookings.stream()
            .map(Booking::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalPaidAmount = userBookings.stream()
            .map(Booking::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    Map<String, Integer> byStatus = userBookings.stream()
            .collect(Collectors.groupingBy(
                    Booking::getStatus,
                    Collectors.summingInt(b -> 1)
            ));
    
    return BookingSummaryDTO.builder()
            .totalBookings(userBookings.size())
            .totalAmount(totalAmount)
            .totalPaidAmount(totalPaidAmount)
            .byStatus(byStatus)
            .build();
}

// ===== MAPPING METHODS =====

private BookingDetailResponse mapToDetailResponse(Booking booking) {
    return BookingDetailResponse.builder()
            .bookingId(booking.getId())
            .bookingCode(booking.getBookingCode())
            .userId(booking.getUserId())
            .departureId(booking.getDepartureId())
            .totalAmount(booking.getTotalAmount())
            .paidAmount(booking.getPaidAmount())
            .status(booking.getStatus())
            .paymentMethod(booking.getPaymentMethod())
            .paymentDueAt(booking.getPaymentDueAt())
            .contactName(booking.getContactName())
            .contactEmail(booking.getContactEmail())
            .contactPhone(booking.getContactPhone())
            .priceSnapshot(booking.getPriceSnapshot())
            .promotionSnapshot(booking.getPromotionSnapshot())
            .createdAt(booking.getCreatedAt())
            .version(booking.getVersion())
            .passengers(mapPassengers(booking.getPassengers()))
            .bookingNotes(mapBookingNotes(booking.getBookingNotes()))
            .cancellation(mapCancellation(booking.getCancellation()))
            .build();
}

private BookingResponse mapToResponse(Booking booking) {
    return BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingCode(booking.getBookingCode())
            .status(booking.getStatus())
            .totalAmount(booking.getTotalAmount())
            .createdAt(booking.getCreatedAt())
            .paymentMethod(booking.getPaymentMethod())
            .paymentDueAt(booking.getPaymentDueAt())
            .userId(booking.getUserId())
            .passengers(mapPassengers(booking.getPassengers()))
            .build();
}

private List<PassengerDTO> mapPassengers(List<Passenger> passengers) {
    if (passengers == null || passengers.isEmpty()) {
        return new ArrayList<>();
    }
    
    return passengers.stream()
            .map(this::mapToPassengerDTO)
            .collect(Collectors.toList());
}

private PassengerDTO mapToPassengerDTO(Passenger p) {
    return PassengerDTO.builder()
            .passengerId(p.getId())
            .fullName(p.getFullName())
            .dob(p.getDob())
            .gender(p.getGender())
            .ageGroup(p.getAgeGroup())
            .idCardNumber(p.getIdCardNumber())
            .passportNumber(p.getPassportNumber())
            .build();
}

private List<BookingNoteDTO> mapBookingNotes(List<BookingNote> notes) {
    if (notes == null || notes.isEmpty()) {
        return new ArrayList<>();
    }
    
    return notes.stream()
            .map(this::mapToBookingNoteDTO)
            .collect(Collectors.toList());
}

private BookingNoteDTO mapToBookingNoteDTO(BookingNote note) {
    return BookingNoteDTO.builder()
            .noteId(note.getId())
            .content(note.getContent())
            .createdAt(note.getCreatedAt())
            .build();
}

private CancellationDTO mapToCancellationDTO(Cancellation c) {
    if (c == null) return null;
    
    return CancellationDTO.builder()
            .cancellationId(c.getId())
            .bookingId(c.getBookingId())
            .reason(c.getReason())
            .refundAmount(c.getRefundAmount())
            .cancelledAt(c.getCancelledAt())
            .build();
}

private CancellationDTO mapCancellation(Cancellation cancellation) {
    return mapToCancellationDTO(cancellation);
}
```

---

## 4. Controller Template

### Add to `BookingController.java`
```java
// Add these imports
import com.tour.booking.dto.request.BatchBookingRequest;
import com.tour.booking.dto.response.*;
import org.springframework.http.ResponseEntity;

// ===== Add these endpoint methods to BookingController class =====

@GetMapping("/{bookingCode}")
public ResponseEntity<ApiResponse<BookingDetailResponse>> getByCode(
        @PathVariable String bookingCode) {
    
    log.info("GET /api/v1/bookings/{}", bookingCode);
    
    BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);
    
    return ResponseEntity.ok(ApiResponse.<BookingDetailResponse>builder()
            .status(200)
            .message("Lấy chi tiết đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping("/id/{bookingId}")
public ResponseEntity<ApiResponse<BookingDetailResponse>> getById(
        @PathVariable Long bookingId) {
    
    log.info("GET /api/v1/bookings/id/{}", bookingId);
    
    BookingDetailResponse response = bookingService.getBookingById(bookingId);
    
    return ResponseEntity.ok(ApiResponse.<BookingDetailResponse>builder()
            .status(200)
            .message("Lấy chi tiết đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping("/user/{userId}")
public ResponseEntity<ApiResponse<PaginatedResponse<BookingResponse>>> getByUserId(
        @PathVariable Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer limit) {
    
    log.info("GET /api/v1/bookings/user/{}, status={}, page={}, limit={}", 
            userId, status, page, limit);
    
    PaginatedResponse<BookingResponse> response = bookingService
            .getBookingsByUserId(userId, status, page, limit);
    
    return ResponseEntity.ok(ApiResponse.<PaginatedResponse<BookingResponse>>builder()
            .status(200)
            .message("Lấy danh sách đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping
public ResponseEntity<ApiResponse<PaginatedResponse<BookingResponse>>> getAll(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentMethod,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer limit) {
    
    log.info("GET /api/v1/bookings, status={}, paymentMethod={}, page={}, limit={}", 
            status, paymentMethod, page, limit);
    
    PaginatedResponse<BookingResponse> response = bookingService
            .getAllBookings(status, paymentMethod, page, limit);
    
    return ResponseEntity.ok(ApiResponse.<PaginatedResponse<BookingResponse>>builder()
            .status(200)
            .message("Lấy danh sách đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping("/{bookingCode}/passengers")
public ResponseEntity<ApiResponse<List<PassengerDTO>>> getPassengers(
        @PathVariable String bookingCode) {
    
    log.info("GET /api/v1/bookings/{}/passengers", bookingCode);
    
    List<PassengerDTO> response = bookingService
            .getPassengersByBookingCode(bookingCode);
    
    return ResponseEntity.ok(ApiResponse.<List<PassengerDTO>>builder()
            .status(200)
            .message("Lấy danh sách hành khách thành công")
            .data(response)
            .build());
}

@GetMapping("/{bookingCode}/notes")
public ResponseEntity<ApiResponse<List<BookingNoteDTO>>> getNotes(
        @PathVariable String bookingCode) {
    
    log.info("GET /api/v1/bookings/{}/notes", bookingCode);
    
    List<BookingNoteDTO> response = bookingService
            .getBookingNotesByBookingCode(bookingCode);
    
    return ResponseEntity.ok(ApiResponse.<List<BookingNoteDTO>>builder()
            .status(200)
            .message("Lấy ghi chú đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping("/{bookingCode}/cancellation")
public ResponseEntity<ApiResponse<CancellationDTO>> getCancellation(
        @PathVariable String bookingCode) {
    
    log.info("GET /api/v1/bookings/{}/cancellation", bookingCode);
    
    CancellationDTO response = bookingService
            .getCancellationByBookingCode(bookingCode);
    
    return ResponseEntity.ok(ApiResponse.<CancellationDTO>builder()
            .status(200)
            .message("Lấy thông tin hủy đơn thành công")
            .data(response)
            .build());
}

@PostMapping("/batch")
public ResponseEntity<ApiResponse<Map<Long, BookingDetailResponse>>> getBatch(
        @RequestBody BatchBookingRequest request) {
    
    log.info("POST /api/v1/bookings/batch, ids={}", request.getBookingIds());
    
    Map<Long, BookingDetailResponse> response = bookingService
            .getBookingsByIds(request.getBookingIds());
    
    return ResponseEntity.ok(ApiResponse.<Map<Long, BookingDetailResponse>>builder()
            .status(200)
            .message("Lấy danh sách đơn hàng thành công")
            .data(response)
            .build());
}

@GetMapping("/user/{userId}/summary")
public ResponseEntity<ApiResponse<BookingSummaryDTO>> getUserSummary(
        @PathVariable Long userId) {
    
    log.info("GET /api/v1/bookings/user/{}/summary", userId);
    
    BookingSummaryDTO response = bookingService.getUserBookingSummary(userId);
    
    return ResponseEntity.ok(ApiResponse.<BookingSummaryDTO>builder()
            .status(200)
            .message("Lấy thống kê đơn hàng thành công")
            .data(response)
            .build());
}
```

---

## 5. Database Index Migration Script

### File: `V3__booking_get_api_indexes.sql`
```sql
-- Create indexes for GET API performance optimization

-- 1. Index on booking_code for direct lookup
CREATE UNIQUE INDEX idx_bookings_code ON bookings(booking_code);

-- 2. Index on user_id for user's bookings queries
CREATE INDEX idx_bookings_user_id ON bookings(user_id);

-- 3. Index on status for filtering
CREATE INDEX idx_bookings_status ON bookings(status);

-- 4. Index on created_at for sorting
CREATE INDEX idx_bookings_created_at ON bookings(created_at DESC);

-- 5. Composite index for user_id + status + created_at queries
CREATE INDEX idx_bookings_user_status_created 
ON bookings(user_id, status, created_at DESC);

-- 6. Index on payment_method for filtering
CREATE INDEX idx_bookings_payment_method ON bookings(payment_method);

-- 7. Index on foreign key (passengers)
CREATE INDEX idx_passengers_booking_id ON passenger(booking_id);

-- 8. Index on foreign key (booking_notes)
CREATE INDEX idx_booking_notes_booking_id ON booking_notes(booking_id);

-- 9. Index on foreign key (cancellations)
CREATE INDEX idx_cancellations_booking_id ON cancellations(booking_id);

-- 10. Composite index for common admin queries
CREATE INDEX idx_bookings_status_payment_created 
ON bookings(status, payment_method, created_at DESC);
```

---

## 6. Quick Implementation Checklist

```markdown
### Phase 1: DTOs & Repositories (Day 1-2)
- [ ] Create BookingDetailResponse.java
- [ ] Create BookingNoteDTO.java
- [ ] Create CancellationDTO.java
- [ ] Create BookingSummaryDTO.java
- [ ] Create PaginatedResponse.java
- [ ] Create BatchBookingRequest.java
- [ ] Update BookingRepository with new query methods
- [ ] Create PassengerRepository
- [ ] Create BookingNoteRepository
- [ ] Create CancellationRepository

### Phase 2: Service Implementation (Day 2-3)
- [ ] Add method signatures to BookingService interface
- [ ] Implement getBookingByCode()
- [ ] Implement getBookingById()
- [ ] Implement getBookingsByUserId() with pagination
- [ ] Implement getAllBookings() with filters
- [ ] Implement getPassengersByBookingCode()
- [ ] Implement getBookingNotesByBookingCode()
- [ ] Implement getCancellationByBookingCode()
- [ ] Implement getBookingsByIds() - Batch
- [ ] Implement getUserBookingSummary()
- [ ] Add mapping methods (mapToDetailResponse, etc.)

### Phase 3: Controller (Day 3)
- [ ] Add all GET endpoints to BookingController
- [ ] Add proper logging
- [ ] Add proper exception handling
- [ ] Add input validation

### Phase 4: Database & Performance (Day 4)
- [ ] Create Flyway migration V3__booking_get_api_indexes.sql
- [ ] Run migration to create indexes
- [ ] Add caching layer (Redis) if needed
- [ ] Configure HikariCP connection pool

### Phase 5: Testing (Day 5)
- [ ] Write unit tests for service layer
- [ ] Write integration tests for controller
- [ ] Test pagination edge cases
- [ ] Test error scenarios (404, 400, 500)
- [ ] Load test with large datasets

### Phase 6: Documentation & Deployment (Day 6)
- [ ] Update Swagger/OpenAPI docs
- [ ] Create API documentation
- [ ] Code review
- [ ] Deploy to staging
- [ ] Deploy to production
```

