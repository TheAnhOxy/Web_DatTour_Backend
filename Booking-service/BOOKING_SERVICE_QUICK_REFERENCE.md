# Booking Service - Quick Reference Guide

**Chuẩn bị code Booking Service GET APIs nhanh chóng**

---

## 📌 Tóm Tắt Trong 2 Phút

### Mục Tiêu
Implement 9 GET API endpoints để lấy dữ liệu từ Neon Database, theo cấu trúc `booking_db_schema.md`.

### API Endpoints (9 cái)
```
1. GET  /api/v1/bookings/{bookingCode}               → Lấy 1 đơn theo mã
2. GET  /api/v1/bookings/id/{bookingId}              → Lấy 1 đơn theo ID
3. GET  /api/v1/bookings/user/{userId}               → Lấy danh sách (phân trang)
4. GET  /api/v1/bookings                             → Lấy all (admin, phân trang)
5. GET  /api/v1/bookings/{bookingCode}/passengers    → Lấy hành khách
6. GET  /api/v1/bookings/{bookingCode}/notes         → Lấy ghi chú
7. GET  /api/v1/bookings/{bookingCode}/cancellation  → Lấy hủy đơn
8. POST /api/v1/bookings/batch                       → Lấy nhiều đơn (batch)
9. GET  /api/v1/bookings/user/{userId}/summary       → Thống kê đơn
```

### 4 Layers (từ trên xuống)
```
Controller (9 endpoints) → Service (10 methods) → Repository (10+ queries) → Database
```

### 6 DTOs Cần Tạo
```
BookingDetailResponse, BookingNoteDTO, CancellationDTO, 
BookingSummaryDTO, PaginatedResponse, BatchBookingRequest
```

### Database (Neon PostgreSQL)
```
bookings ← bookings.id → passengers
       ↓               → booking_notes
       ↓               → cancellations
       ↑
   Giữ dữ liệu về:
   - booking_code (UNIQUE key)
   - user_id, departure_id, status, payment_method
   - priceSnapshot, promotionSnapshot (JSON)
   - version (Optimistic Locking)
```

---

## 📋 Checklist Nhanh (Copy-Paste Friendly)

### Day 1: DTOs & Repos
```
DTOs (6 files):
□ BookingDetailResponse.java
□ BookingNoteDTO.java
□ CancellationDTO.java
□ BookingSummaryDTO.java
□ PaginatedResponse.java
□ BatchBookingRequest.java

Repos (4 files):
□ Update BookingRepository (add 10 query methods)
□ Create PassengerRepository
□ Create BookingNoteRepository
□ Create CancellationRepository
```

### Day 2: Service Layer
```
BookingService interface:
□ Add 10 method signatures

BookingServiceImpl:
□ Implement 10 methods
□ Add 8 mapping methods (mapToDetailResponse, etc.)
□ Add @Transactional(readOnly=true)
□ Add logging
```

### Day 3: Controller
```
BookingController:
□ Add 9 GET/POST endpoints
□ Add request validation
□ Add exception handling
□ Test all endpoints
```

### Day 4: Database & Cache
```
□ Create V3__booking_get_api_indexes.sql (10 indexes)
□ Create CacheConfig.java
□ Configure Redis (30min TTL)
□ Test query performance
```

### Day 5: Tests
```
□ Create BookingServiceImplTest.java (10 test methods)
□ Create BookingControllerIntegrationTest.java (9 test methods)
□ Achieve 80%+ code coverage
```

### Day 6: Deploy
```
□ Update OpenAPI/Swagger
□ Create API documentation
□ Code review
□ Deploy to staging
□ Deploy to production
```

---

## 🔧 Code Snippets Quick Ref

### Quick Service Method Template
```java
@Override
@Transactional(readOnly = true)
public BookingDetailResponse getBookingByCode(String bookingCode) {
    Booking booking = bookingRepository.findByBookingCodeFetchAll(bookingCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy đơn hàng: " + bookingCode));
    
    return mapToDetailResponse(booking);
}
```

### Quick Controller Endpoint Template
```java
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
```

### Quick Repository Query Template
```java
@Query("SELECT DISTINCT b FROM Booking b " +
       "LEFT JOIN FETCH b.passengers " +
       "LEFT JOIN FETCH b.bookingNotes " +
       "LEFT JOIN FETCH b.cancellation " +
       "WHERE b.bookingCode = :bookingCode")
Optional<Booking> findByBookingCodeFetchAll(@Param("bookingCode") String bookingCode);
```

### Quick Mapping Template
```java
private BookingDetailResponse mapToDetailResponse(Booking booking) {
    return BookingDetailResponse.builder()
            .bookingId(booking.getId())
            .bookingCode(booking.getBookingCode())
            .status(booking.getStatus())
            .totalAmount(booking.getTotalAmount())
            // ... other fields
            .passengers(mapPassengers(booking.getPassengers()))
            .bookingNotes(mapBookingNotes(booking.getBookingNotes()))
            .cancellation(mapCancellation(booking.getCancellation()))
            .build();
}
```

---

## ⚡ Performance Checklist

### N+1 Query Prevention
```
✓ Use LEFT JOIN FETCH in repository queries
✗ Don't do: List<Booking> bookings = repo.findAll(); 
  then loop and call: booking.getPassengers()
```

### Pagination Best Practices
```
✓ Always use Pageable for list endpoints
✓ Limit default: 10-20 records
✓ Include totalPages, currentPage, hasNext in response
```

### Index Strategy
```sql
-- Essential indexes:
CREATE UNIQUE INDEX idx_bookings_code ON bookings(booking_code);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_created_at ON bookings(created_at DESC);
CREATE INDEX idx_bookings_user_status_created ON bookings(user_id, status, created_at DESC);
```

### Caching Strategy
```
Cache key: booking:{bookingCode}
TTL: 30 minutes
Hit rate target: 70-80%
Invalidate on: booking update/cancel
```

---

## 📊 Query Performance Expectations

| Endpoint | Expected Time | With Cache |
|----------|---------------|-----------|
| GET /bookings/{code} | 10-20ms | <5ms |
| GET /bookings/user/{id} | 50-100ms | 20-50ms |
| GET /bookings (all) | 100-300ms | 50-150ms |
| GET /bookings/batch | 20-50ms per booking | 5-20ms |

---

## 🐛 Common Issues & Fixes

### Issue 1: N+1 Queries
**Symptom**: One query returns 10 bookings, then 10 more queries for passengers
**Fix**: Use `LEFT JOIN FETCH b.passengers` in repository

### Issue 2: Pagination Not Working
**Symptom**: getAllBookings() returns wrong page count
**Fix**: Make sure Pageable is passed to repository method

### Issue 3: Cache Not Working
**Symptom**: Always hitting database, never cache hit
**Fix**: Ensure Redis is running, check connection in logs

### Issue 4: Missing Cancellation Info
**Symptom**: CancellationDTO is null even if booking was cancelled
**Fix**: Add `LEFT JOIN FETCH b.cancellation` to query

### Issue 5: Slow All Bookings Query (Admin)
**Symptom**: getAllBookings() with filters takes >500ms
**Fix**: Create composite index on (status, payment_method, created_at)

---

## 📚 File References

### Read First (Understanding)
1. [booking_db_schema.md](booking_db_schema.md) - Database structure
2. [BOOKING_SERVICE_GET_API_IMPLEMENTATION.md](BOOKING_SERVICE_GET_API_IMPLEMENTATION.md) - Full guide

### Copy-Paste From (Implementation)
1. [BOOKING_SERVICE_CODE_TEMPLATES.md](BOOKING_SERVICE_CODE_TEMPLATES.md) - Code templates

### Reference During (Execution)
1. [BOOKING_SERVICE_ARCHITECTURE_PLAN.md](BOOKING_SERVICE_ARCHITECTURE_PLAN.md) - Timeline & architecture

### Current Project Files
- `BookingService.java` - Service interface
- `BookingServiceImpl.java` - Service implementation
- `BookingRepository.java` - Repository
- `BookingController.java` - REST controller
- `Booking.java` - Entity

---

## 🎯 Step-by-Step (Copy-Paste Friendly)

### Step 1: Create DTOs
```bash
# Create these 6 files in src/main/java/com/tour/booking/dto/response/
1. BookingDetailResponse.java
2. BookingNoteDTO.java
3. CancellationDTO.java
4. BookingSummaryDTO.java
5. PaginatedResponse.java

# Create this file in src/main/java/com/tour/booking/dto/request/
6. BatchBookingRequest.java
```

### Step 2: Update Repository
```bash
# Edit: src/main/java/com/tour/booking/repository/BookingRepository.java
# Add 10 new @Query methods

# Create: src/main/java/com/tour/booking/repository/PassengerRepository.java
# Create: src/main/java/com/tour/booking/repository/BookingNoteRepository.java
# Create: src/main/java/com/tour/booking/repository/CancellationRepository.java
```

### Step 3: Update Service
```bash
# Edit: src/main/java/com/tour/booking/service/BookingService.java
# Add 10 method signatures

# Edit: src/main/java/com/tour/booking/service/impl/BookingServiceImpl.java
# Add 10 implementations + 8 mapping methods
```

### Step 4: Update Controller
```bash
# Edit: src/main/java/com/tour/booking/controller/BookingController.java
# Add 9 endpoint methods
```

### Step 5: Database
```bash
# Create: src/main/resources/db/migration/V3__booking_get_api_indexes.sql
# Add 10 indexes for performance
# Run migration: ./mvnw flyway:migrate
```

### Step 6: Testing & Deploy
```bash
# Create: src/test/java/com/tour/booking/service/BookingServiceImplTest.java
# Create: src/test/java/com/tour/booking/controller/BookingControllerIntegrationTest.java
# Run: ./mvnw clean test
# Build: ./mvnw clean package
# Deploy to Neon
```

---

## 🔐 Security & Validation

### Input Validation
```java
// In controller
@PathVariable @NotBlank String bookingCode
@RequestParam(required = false) @Pattern(regexp="PENDING|CONFIRMED|...") String status
@RequestParam @Min(1) Long userId
```

### Authorization (If Needed)
```java
// Check if user accessing their own bookings
if (!booking.getUserId().equals(currentUserId)) {
    throw new ForbiddenException("Không có quyền truy cập");
}
```

### Sensitive Data Filtering
```java
// Don't expose priceSnapshot details unnecessarily
// Don't expose paymentMethod if booking cancelled
if (booking.getStatus().equals("CANCELLED")) {
    response.setPaymentMethod(null);
}
```

---

## 📱 Frontend Integration Examples

### Example 1: Get Booking by Code (from Frontend)
```javascript
// Frontend (Next.js)
const response = await fetch(`/api/v1/bookings/BKG123456`);
const { data } = await response.json();
```

### Example 2: Get User Bookings (Paginated)
```javascript
const response = await fetch(
    `/api/v1/bookings/user/123?status=PENDING&page=0&limit=10`
);
const { data } = await response.json();
// data.hasNext indicates if there are more pages
```

### Example 3: Get Batch Bookings
```javascript
const response = await fetch(`/api/v1/bookings/batch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ bookingIds: [1, 2, 3] })
});
```

---

## 🚀 Deployment Commands

### Local Testing
```bash
# 1. Start application
./mvnw spring-boot:run

# 2. Test endpoints
curl http://localhost:8082/api/v1/bookings/BKG123456

# 3. View logs
tail -f logs/application.log
```

### Build for Production
```bash
# Clean build
./mvnw clean package

# Build Docker image (if using Docker)
docker build -t booking-service:1.0 .

# Deploy
docker push your-registry/booking-service:1.0
```

### Database Migration (Neon)
```bash
# Create migration file
touch src/main/resources/db/migration/V3__booking_get_api_indexes.sql

# Run migration
./mvnw flyway:migrate

# Rollback (if needed)
./mvnw flyway:undo
```

---

## 💡 Pro Tips

1. **Use @Transactional(readOnly=true)** on all GET methods for better performance
2. **Always use Pageable** for list endpoints to prevent large data transfers
3. **Use LEFT JOIN FETCH** to prevent N+1 queries
4. **Index composite queries** like (user_id, status, created_at)
5. **Cache frequently accessed data** (bookings by code: 90% cache hit)
6. **Monitor query performance** - target <100ms for all GET endpoints
7. **Use OpenAPI/Swagger** for API documentation
8. **Write integration tests** before deployment
9. **Use connection pooling** (HikariCP) for database
10. **Enable slow query log** in PostgreSQL for debugging

---

## 📞 Support Resources

### If You Get Stuck:
1. Check [BOOKING_SERVICE_GET_API_IMPLEMENTATION.md](BOOKING_SERVICE_GET_API_IMPLEMENTATION.md) for detailed explanation
2. Copy code from [BOOKING_SERVICE_CODE_TEMPLATES.md](BOOKING_SERVICE_CODE_TEMPLATES.md)
3. Reference architecture in [BOOKING_SERVICE_ARCHITECTURE_PLAN.md](BOOKING_SERVICE_ARCHITECTURE_PLAN.md)

### Common Questions:

**Q: Should I create all DTOs at once?**
A: Yes, do all 6 DTOs together in ~30 min

**Q: What if pagination breaks?**
A: Make sure totalPages is calculated as `Math.ceil(total / limit)`

**Q: How to test without frontend?**
A: Use Postman, curl, or Spring Boot's built-in REST client

**Q: Can I use caching without Redis?**
A: Yes, use Spring's ConcurrentMapCacheManager, but Redis is better for production

**Q: When should I create indexes?**
A: After repositories are ready (Day 1-2), before testing (Day 4)

---

## 📈 Success Metrics

After implementation, your Booking Service should have:
- ✅ 9 working GET endpoints
- ✅ <100ms average response time
- ✅ 70%+ cache hit rate
- ✅ 0 N+1 query issues
- ✅ 80%+ test coverage
- ✅ Proper error handling
- ✅ Complete API documentation

**Total Implementation Time: 6 days (30-40 hours)**

---

**Last Updated**: May 24, 2026
**Version**: 1.0
**Status**: Ready for Implementation

