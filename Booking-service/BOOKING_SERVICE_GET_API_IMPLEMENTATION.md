# Booking Service - GET API Implementation Guide

Tài liệu này cung cấp hướng dẫn chi tiết để chuẩn bị code Booking Service GET APIs theo cấu trúc database (booking_db) trên Neon DB.

---

## I. API Endpoints Overview

### 1. **Get Booking by Code** (Lấy đơn hàng theo Booking Code)
```
GET /api/v1/bookings/{bookingCode}
```
- **Purpose**: Lấy chi tiết đầy đủ một đơn hàng dựa trên mã booking
- **Response**: `BookingDetailResponse` (bao gồm thông tin booking + hành khách + ghi chú)
- **Status Code**: 200 (OK), 404 (Not Found), 500 (Server Error)

### 2. **Get Booking by ID** (Lấy đơn hàng theo ID)
```
GET /api/v1/bookings/id/{bookingId}
```
- **Purpose**: Lấy chi tiết đơn hàng theo ID chính
- **Response**: `BookingDetailResponse`

### 3. **Get Bookings by User ID** (Lấy tất cả đơn hàng của người dùng)
```
GET /api/v1/bookings/user/{userId}?status=PENDING&limit=10&offset=0
```
- **Purpose**: Lấy danh sách đơn hàng của một người dùng (có phân trang + filter)
- **Query Parameters**:
  - `status` (optional): PENDING, CONFIRMED, CANCELLED, PAID, COMPLETED
  - `limit` (default: 10): Số lượng record trên một trang
  - `offset` (default: 0): Vị trí bắt đầu
- **Response**: `PaginatedResponse<BookingResponse>`

### 4. **Get All Bookings** (Lấy tất cả đơn hàng - Admin)
```
GET /api/v1/bookings?status=PENDING&userId=123&limit=20&offset=0
```
- **Purpose**: Lấy danh sách tất cả đơn hàng (Admin use case)
- **Query Parameters**:
  - `status` (optional): Lọc theo trạng thái
  - `userId` (optional): Lọc theo user ID
  - `paymentMethod` (optional): CREDIT_CARD, VNPAY, MOMO, BANK_TRANSFER, CASH_OFFICE
  - `limit`, `offset`: Phân trang
- **Response**: `PaginatedResponse<BookingResponse>`

### 5. **Get Booking Details** (Lấy chi tiết hoàn chỉnh)
```
GET /api/v1/bookings/{bookingCode}/details
```
- **Purpose**: Lấy thông tin chi tiết kèm hành khách, ghi chú, hủy đơn
- **Response**: `BookingDetailResponse` (kèm passengers, bookingNotes, cancellation)

### 6. **Get Passengers by Booking** (Lấy danh sách hành khách)
```
GET /api/v1/bookings/{bookingCode}/passengers
```
- **Purpose**: Lấy danh sách hành khách của một đơn hàng
- **Response**: `List<PassengerDTO>`

### 7. **Get Booking Notes** (Lấy ghi chú đơn hàng)
```
GET /api/v1/bookings/{bookingCode}/notes
```
- **Purpose**: Lấy tất cả ghi chú liên quan đến đơn hàng
- **Response**: `List<BookingNoteDTO>`

### 8. **Get Cancellation Info** (Lấy thông tin hủy đơn)
```
GET /api/v1/bookings/{bookingCode}/cancellation
```
- **Purpose**: Lấy thông tin hủy đơn (nếu có)
- **Response**: `CancellationDTO` hoặc `null` nếu chưa hủy

### 9. **Get Bookings by Multiple IDs** (Lấy nhiều đơn hàng cùng lúc)
```
POST /api/v1/bookings/batch
Content-Type: application/json

{
  "bookingIds": [1, 2, 3, 4]
}
```
- **Purpose**: Lấy nhiều đơn hàng theo danh sách ID
- **Response**: `Map<Long, BookingDetailResponse>`

### 10. **Get User Bookings Summary** (Thống kê đơn hàng người dùng)
```
GET /api/v1/bookings/user/{userId}/summary
```
- **Purpose**: Lấy thống kê: tổng số đơn, tổng tiền, phân loại theo trạng thái
- **Response**:
```json
{
  "totalBookings": 5,
  "totalAmount": 15000000,
  "totalPaidAmount": 10000000,
  "byStatus": {
    "PENDING": 1,
    "CONFIRMED": 2,
    "PAID": 1,
    "CANCELLED": 1
  }
}
```

---

## II. DTO Structures

### 2.1 BookingDetailResponse (Response cho GET requests)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {
    // Booking Info
    private Long bookingId;
    private String bookingCode;
    private Long userId;
    private Long departureId;
    
    // Amount Info
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    
    // Status & Payment
    private String status;              // PENDING, CONFIRMED, CANCELLED, PAID, COMPLETED
    private String paymentMethod;       // CREDIT_CARD, VNPAY, MOMO, BANK_TRANSFER, CASH_OFFICE
    private LocalDateTime paymentDueAt;
    
    // Contact Info
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    
    // Price & Promotion Details (JSON)
    private Map<String, Object> priceSnapshot;
    private Map<String, Object> promotionSnapshot;
    
    // Temporal Info
    private LocalDateTime createdAt;
    private Long version;
    
    // Related Data
    private List<PassengerDTO> passengers;
    private List<BookingNoteDTO> bookingNotes;
    private CancellationDTO cancellation;
}
```

### 2.2 PassengerDTO (Thông tin hành khách)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDTO {
    private Long passengerId;
    private String fullName;
    private LocalDate dob;
    private String gender;              // MALE, FEMALE, OTHER
    private String ageGroup;            // ADULT, CHILD, INFANT
    private String idCardNumber;
    private String passportNumber;
}
```

### 2.3 BookingNoteDTO (Ghi chú đơn hàng)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingNoteDTO {
    private Long noteId;
    private String content;
    private LocalDateTime createdAt;
}
```

### 2.4 CancellationDTO (Thông tin hủy đơn)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDTO {
    private Long cancellationId;
    private Long bookingId;
    private String reason;
    private BigDecimal refundAmount;
    private LocalDateTime cancelledAt;
}
```

### 2.5 BookingSummaryDTO (Thống kê)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDTO {
    private Integer totalBookings;
    private BigDecimal totalAmount;
    private BigDecimal totalPaidAmount;
    private Map<String, Integer> byStatus;  // {PENDING: 1, CONFIRMED: 2, ...}
}
```

### 2.6 PaginatedResponse<T> (Phản hồi phân trang)
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> data;
    private Integer totalElements;
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNext;
}
```

---

## III. Repository Layer Implementation

### 3.1 BookingRepository - Custom Query Methods

```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // 1. Find by Booking Code (with passengers + notes + cancellation)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.bookingCode = :bookingCode")
    Optional<Booking> findByBookingCodeFetchAll(@Param("bookingCode") String bookingCode);
    
    // 2. Find by ID (with all related data)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdFetchAll(@Param("id") Long id);
    
    // 3. Find by User ID (with passengers, paginated)
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
    
    // 5. Find all bookings (admin - with passengers)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithPassengers(Pageable pageable);
    
    // 6. Find all by status (admin)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByStatusWithPassengers(
            @Param("status") String status, 
            Pageable pageable);
    
    // 7. Find by multiple IDs (batch)
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
    
    // 10. Find by Payment Method and Due Date (Cleanup Task)
    @Query("SELECT b FROM Booking b " +
           "WHERE b.status = 'PENDING' " +
           "AND b.paymentMethod = :paymentMethod " +
           "AND b.paymentDueAt < :dueDate " +
           "ORDER BY b.createdAt ASC")
    List<Booking> findPendingByPaymentMethodAndDueDate(
            @Param("paymentMethod") String paymentMethod,
            @Param("dueDate") LocalDateTime dueDate,
            Pageable pageable);
}
```

### 3.2 PassengerRepository
```java
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    // Find all passengers by booking
    List<Passenger> findByBookingId(Long bookingId);
    
    // Find by booking code (via native query)
    @Query(value = "SELECT p.* FROM passenger p " +
                   "JOIN bookings b ON p.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode", 
           nativeQuery = true)
    List<Passenger> findByBookingCode(@Param("bookingCode") String bookingCode);
}
```

### 3.3 BookingNoteRepository
```java
@Repository
public interface BookingNoteRepository extends JpaRepository<BookingNote, Long> {
    
    // Find all notes by booking
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

### 3.4 CancellationRepository
```java
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

## IV. Service Layer Implementation

### 4.1 Interface Methods (BookingService)
```java
public interface BookingService {
    
    // === GET Operations ===
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
    
    // === CRUD Operations ===
    BookingResponse createBooking(BookingRequest request);
    void cancelBooking(CancelBookingRequest request);
}
```

### 4.2 Implementation (BookingServiceImpl)

#### GET by Code
```java
@Override
@Transactional(readOnly = true)
public BookingDetailResponse getBookingByCode(String bookingCode) {
    Booking booking = bookingRepository.findByBookingCodeFetchAll(bookingCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy đơn hàng với mã: " + bookingCode));
    
    return mapToDetailResponse(booking);
}

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
```

#### GET by User ID (Paginated)
```java
@Override
@Transactional(readOnly = true)
public PaginatedResponse<BookingResponse> getBookingsByUserId(
        Long userId, 
        String status, 
        Integer page, 
        Integer limit) {
    
    Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
    
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
    
    return PaginatedResponse.<BookingResponse>builder()
            .data(responses)
            .totalElements(total.intValue())
            .currentPage(page)
            .pageSize(limit)
            .totalPages((int) Math.ceil((double) total / limit))
            .hasNext((page + 1) * limit < total)
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
```

#### GET All Bookings (Admin - Paginated)
```java
@Override
@Transactional(readOnly = true)
public PaginatedResponse<BookingResponse> getAllBookings(
        String status, 
        String paymentMethod, 
        Integer page, 
        Integer limit) {
    
    Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
    
    // Implementation sử dụng Specification nếu nhiều filter
    Specification<Booking> spec = Specification.where(null);
    
    if (status != null && !status.isEmpty()) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    
    if (paymentMethod != null && !paymentMethod.isEmpty()) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentMethod"), paymentMethod));
    }
    
    Page<Booking> page_result = bookingRepository.findAll(spec, pageable);
    
    List<BookingResponse> responses = page_result.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    
    return PaginatedResponse.<BookingResponse>builder()
            .data(responses)
            .totalElements((int) page_result.getTotalElements())
            .currentPage(page)
            .pageSize(limit)
            .totalPages(page_result.getTotalPages())
            .hasNext(page_result.hasNext())
            .build();
}
```

#### GET Passengers
```java
@Override
@Transactional(readOnly = true)
public List<PassengerDTO> getPassengersByBookingCode(String bookingCode) {
    List<Passenger> passengers = passengerRepository.findByBookingCode(bookingCode);
    
    if (passengers.isEmpty()) {
        throw new ResourceNotFoundException("Không tìm thấy hành khách cho đơn hàng: " + bookingCode);
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
```

#### GET Booking Summary
```java
@Override
@Transactional(readOnly = true)
public BookingSummaryDTO getUserBookingSummary(Long userId) {
    List<Booking> userBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    
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
```

---

## V. Controller Layer Implementation

```java
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    
    private final BookingService bookingService;
    
    // 1. Get by Booking Code
    @GetMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getByCode(
            @PathVariable String bookingCode) {
        BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);
        
        return ResponseEntity.ok(ApiResponse.<BookingDetailResponse>builder()
                .status(200)
                .message("Lấy chi tiết đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 2. Get by ID
    @GetMapping("/id/{bookingId}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getById(
            @PathVariable Long bookingId) {
        BookingDetailResponse response = bookingService.getBookingById(bookingId);
        
        return ResponseEntity.ok(ApiResponse.<BookingDetailResponse>builder()
                .status(200)
                .message("Lấy chi tiết đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 3. Get Bookings by User ID (Paginated)
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PaginatedResponse<BookingResponse>>> getByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        PaginatedResponse<BookingResponse> response = bookingService
                .getBookingsByUserId(userId, status, page, limit);
        
        return ResponseEntity.ok(ApiResponse.<PaginatedResponse<BookingResponse>>builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 4. Get All Bookings (Admin)
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BookingResponse>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        
        PaginatedResponse<BookingResponse> response = bookingService
                .getAllBookings(status, paymentMethod, page, limit);
        
        return ResponseEntity.ok(ApiResponse.<PaginatedResponse<BookingResponse>>builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 5. Get Passengers
    @GetMapping("/{bookingCode}/passengers")
    public ResponseEntity<ApiResponse<List<PassengerDTO>>> getPassengers(
            @PathVariable String bookingCode) {
        
        List<PassengerDTO> response = bookingService
                .getPassengersByBookingCode(bookingCode);
        
        return ResponseEntity.ok(ApiResponse.<List<PassengerDTO>>builder()
                .status(200)
                .message("Lấy danh sách hành khách thành công")
                .data(response)
                .build());
    }
    
    // 6. Get Booking Notes
    @GetMapping("/{bookingCode}/notes")
    public ResponseEntity<ApiResponse<List<BookingNoteDTO>>> getNotes(
            @PathVariable String bookingCode) {
        
        List<BookingNoteDTO> response = bookingService
                .getBookingNotesByBookingCode(bookingCode);
        
        return ResponseEntity.ok(ApiResponse.<List<BookingNoteDTO>>builder()
                .status(200)
                .message("Lấy ghi chú đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 7. Get Cancellation Info
    @GetMapping("/{bookingCode}/cancellation")
    public ResponseEntity<ApiResponse<CancellationDTO>> getCancellation(
            @PathVariable String bookingCode) {
        
        CancellationDTO response = bookingService
                .getCancellationByBookingCode(bookingCode);
        
        return ResponseEntity.ok(ApiResponse.<CancellationDTO>builder()
                .status(200)
                .message("Lấy thông tin hủy đơn thành công")
                .data(response)
                .build());
    }
    
    // 8. Get Bookings by Multiple IDs (Batch)
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<Long, BookingDetailResponse>>> getBatch(
            @RequestBody BatchBookingRequest request) {
        
        Map<Long, BookingDetailResponse> response = bookingService
                .getBookingsByIds(request.getBookingIds());
        
        return ResponseEntity.ok(ApiResponse.<Map<Long, BookingDetailResponse>>builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(response)
                .build());
    }
    
    // 9. Get User Booking Summary
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<ApiResponse<BookingSummaryDTO>> getUserSummary(
            @PathVariable Long userId) {
        
        BookingSummaryDTO response = bookingService.getUserBookingSummary(userId);
        
        return ResponseEntity.ok(ApiResponse.<BookingSummaryDTO>builder()
                .status(200)
                .message("Lấy thống kê đơn hàng thành công")
                .data(response)
                .build());
    }
}
```

---

## VI. Caching Strategy (Redis)

### 6.1 Cache Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        return new RedissonSpringCacheManager(redissonClient);
    }
}
```

### 6.2 Caching in Service
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
    
    private final RedissonClient redissonClient;
    private static final String BOOKING_CACHE_PREFIX = "booking:";
    private static final int CACHE_TTL_MINUTES = 30;
    
    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingByCode(String bookingCode) {
        String cacheKey = BOOKING_CACHE_PREFIX + bookingCode;
        
        // Try get from cache
        RBucket<BookingDetailResponse> cacheBucket = redissonClient.getBucket(cacheKey);
        if (cacheBucket.isExists()) {
            log.info("Cache hit for booking code: {}", bookingCode);
            return cacheBucket.get();
        }
        
        // Get from DB
        Booking booking = bookingRepository.findByBookingCodeFetchAll(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        BookingDetailResponse response = mapToDetailResponse(booking);
        
        // Store in cache
        cacheBucket.set(response, Duration.ofMinutes(CACHE_TTL_MINUTES));
        
        return response;
    }
    
    // Invalidate cache when booking is updated
    private void invalidateBookingCache(String bookingCode) {
        String cacheKey = BOOKING_CACHE_PREFIX + bookingCode;
        redissonClient.getBucket(cacheKey).delete();
    }
}
```

---

## VII. Error Handling

### 7.1 Custom Exceptions
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}
```

### 7.2 Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                        .status(404)
                        .message(ex.getMessage())
                        .build());
    }
    
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidData(
            InvalidDataException ex) {
        log.warn("Invalid data: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .status(400)
                        .message(ex.getMessage())
                        .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .status(500)
                        .message("Lỗi hệ thống, vui lòng thử lại sau")
                        .build());
    }
}
```

---

## VIII. Performance Optimization

### 8.1 Neon Database Best Practices
1. **Connection Pooling**: Sử dụng HikariCP (mặc định Spring Boot)
2. **Index Strategy**: Tạo index trên các cột thường được query
3. **Fetch Join**: Sử dụng LEFT JOIN FETCH để tránh N+1 queries
4. **Pagination**: Luôn phân trang cho danh sách lớn

### 8.2 Recommended Indexes
```sql
-- Index trên booking_code (unique lookup)
CREATE UNIQUE INDEX idx_bookings_code ON bookings(booking_code);

-- Index trên user_id (user's bookings)
CREATE INDEX idx_bookings_user_id ON bookings(user_id);

-- Index trên status (filtering)
CREATE INDEX idx_bookings_status ON bookings(status);

-- Index trên created_at (sorting)
CREATE INDEX idx_bookings_created_at ON bookings(created_at DESC);

-- Composite index cho filter + sort
CREATE INDEX idx_bookings_user_status_created 
ON bookings(user_id, status, created_at DESC);

-- Index trên passenger (FK lookup)
CREATE INDEX idx_passengers_booking_id ON passenger(booking_id);

-- Index trên booking_notes (FK lookup)
CREATE INDEX idx_booking_notes_booking_id ON booking_notes(booking_id);

-- Index trên cancellations (FK lookup)
CREATE INDEX idx_cancellations_booking_id ON cancellations(booking_id);
```

### 8.3 N+1 Query Prevention
✅ **Good Practice**:
```java
@Query("SELECT DISTINCT b FROM Booking b " +
       "LEFT JOIN FETCH b.passengers " +
       "WHERE b.userId = :userId")
List<Booking> findByUserIdWithPassengers(Long userId);
```

❌ **Bad Practice**:
```java
List<Booking> bookings = bookingRepository.findByUserId(userId);
// N+1: This will trigger N queries to load passengers for each booking
for (Booking b : bookings) {
    b.getPassengers().size(); // Lazy loading trigger
}
```

---

## IX. Testing Strategy

### 9.1 Unit Tests (Service Layer)
```java
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    
    @Mock
    private BookingRepository bookingRepository;
    
    @InjectMocks
    private BookingServiceImpl bookingService;
    
    @Test
    void testGetBookingByCode_Success() {
        // Arrange
        String bookingCode = "BKG123456";
        Booking booking = Booking.builder()
                .id(1L)
                .bookingCode(bookingCode)
                .build();
        
        when(bookingRepository.findByBookingCodeFetchAll(bookingCode))
                .thenReturn(Optional.of(booking));
        
        // Act
        BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);
        
        // Assert
        assertNotNull(response);
        assertEquals(bookingCode, response.getBookingCode());
        verify(bookingRepository, times(1)).findByBookingCodeFetchAll(bookingCode);
    }
    
    @Test
    void testGetBookingByCode_NotFound() {
        // Arrange
        when(bookingRepository.findByBookingCodeFetchAll("INVALID"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> bookingService.getBookingByCode("INVALID"));
    }
}
```

### 9.2 Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Test
    void testGetBookingByCode_Integration() throws Exception {
        // Setup test data
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode("BKG123456")
                .status("PENDING")
                .build());
        
        // Execute
        mockMvc.perform(get("/api/v1/bookings/BKG123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingCode").value("BKG123456"));
    }
}
```

---

## X. Implementation Checklist

- [ ] Create DTOs (BookingDetailResponse, PassengerDTO, etc.)
- [ ] Update BookingRepository with custom queries
- [ ] Create PassengerRepository, BookingNoteRepository, CancellationRepository
- [ ] Implement Service layer methods
- [ ] Implement Controller endpoints
- [ ] Add exception handling (GlobalExceptionHandler)
- [ ] Configure caching strategy
- [ ] Create database indexes
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Performance testing (pagination, large datasets)
- [ ] Document API endpoints (Swagger/OpenAPI)
- [ ] Code review & optimization

---

## XI. Summary

| Feature | Status | Priority |
|---------|--------|----------|
| Get by Booking Code | Ready to implement | HIGH |
| Get by User ID (Paginated) | Ready to implement | HIGH |
| Get All Bookings (Admin) | Ready to implement | MEDIUM |
| Get Passengers | Ready to implement | MEDIUM |
| Get Booking Notes | Ready to implement | LOW |
| Get Cancellation Info | Ready to implement | MEDIUM |
| Batch Get | Ready to implement | LOW |
| User Summary | Ready to implement | MEDIUM |
| Caching Layer | Ready to implement | MEDIUM |
| Error Handling | Ready to implement | HIGH |

