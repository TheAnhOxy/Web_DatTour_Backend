# Booking Service - Architecture & Execution Plan

---

## I. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Next.js)                               │
│                    (/api/bookings, /bookings)                           │
└───────────────────────────┬─────────────────────────────────────────────┘
                            │ HTTP/REST
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port 8080)                              │
│                    /api/v1/bookings/* routes                            │
└───────────────────────────┬─────────────────────────────────────────────┘
                            │ Load Balance
                            ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                  BOOKING SERVICE (Port 8082)                            │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ CONTROLLER LAYER                                                │   │
│  │ - getByCode(bookingCode)                                        │   │
│  │ - getById(bookingId)                                            │   │
│  │ - getByUserId(userId, status, page, limit)                     │   │
│  │ - getAll(status, paymentMethod, page, limit)                   │   │
│  │ - getPassengers(bookingCode)                                    │   │
│  │ - getNotes(bookingCode)                                         │   │
│  │ - getCancellation(bookingCode)                                  │   │
│  │ - getBatch(bookingIds)                                          │   │
│  │ - getUserSummary(userId)                                        │   │
│  └──────────────────────┬──────────────────────────────────────────┘   │
│                         │                                                │
│  ┌──────────────────────↓──────────────────────────────────────────┐   │
│  │ SERVICE LAYER (Business Logic)                                  │   │
│  │ - BookingServiceImpl                                             │   │
│  │   • Mapping logic (Booking → DTO)                               │   │
│  │   • Filtering & Sorting logic                                   │   │
│  │   • Caching layer integration                                   │   │
│  │   • Exception handling                                          │   │
│  └──────────────────────┬──────────────────────────────────────────┘   │
│                         │                                                │
│  ┌──────────────────────↓──────────────────────────────────────────┐   │
│  │ REPOSITORY LAYER (Data Access)                                 │   │
│  │ - BookingRepository                                             │   │
│  │   • findByBookingCodeFetchAll()                                 │   │
│  │   • findByUserIdWithPassengers()                                │   │
│  │   • findAllWithPassengers()                                     │   │
│  │   • countByUserId()                                             │   │
│  │ - PassengerRepository                                           │   │
│  │ - BookingNoteRepository                                         │   │
│  │ - CancellationRepository                                        │   │
│  └──────────────────────┬──────────────────────────────────────────┘   │
│                         │                                                │
│                    (JPA/Hibernate)                                       │
└───────────────────────────┬─────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ↓               ↓               ↓
      ┌──────────┐    ┌──────────┐    ┌──────────┐
      │ Neon DB  │    │ Redis    │    │ Kafka    │
      │ booking  │    │ Cache    │    │ (Events) │
      │ service  │    │ (30min)  │    │          │
      └──────────┘    └──────────┘    └──────────┘
      (PostgreSQL)
```

---

## II. Data Flow Diagram (GET by Code Example)

```
User Request
    │
    ↓
┌─────────────────────────────────┐
│ GET /api/v1/bookings/BKG123456  │
└────────────┬────────────────────┘
             │
             ↓
┌───────────────────────────────────────┐
│ BookingController.getByCode()         │
│ - Log request                         │
│ - Validate bookingCode                │
└────────┬────────────────────────────┘
         │
         ↓
┌───────────────────────────────────────┐
│ BookingServiceImpl.getByCode()         │
│ 1. Check Redis cache first            │
└────────┬────────────────────────────┘
         │
         ├─→ CACHE HIT? ─→ Return cached response
         │
         └─→ CACHE MISS:
             │
             ↓
        ┌──────────────────────────────┐
        │ BookingRepository             │
        │ .findByBookingCodeFetchAll()  │
        │                              │
        │ Executes SQL:                │
        │ SELECT DISTINCT b FROM ...   │
        │ LEFT JOIN FETCH passengers   │
        │ LEFT JOIN FETCH notes        │
        │ LEFT JOIN FETCH cancellation │
        │ WHERE bookingCode = ?        │
        └────────┬─────────────────────┘
                 │
                 ↓
            ┌──────────────────────┐
            │ Neon Database Query  │
            │ (PostgreSQL)         │
            └────────┬─────────────┘
                     │
                     ↓
            ┌──────────────────────┐
            │ Return Booking Entity│
            │ with related data    │
            └────────┬─────────────┘
                     │
                     ↓
        ┌──────────────────────────────┐
        │ Map Entity → DTOs             │
        │ - mapToDetailResponse()       │
        │ - mapPassengers()             │
        │ - mapBookingNotes()           │
        │ - mapCancellation()           │
        └────────┬─────────────────────┘
                 │
                 ↓
        ┌──────────────────────────────┐
        │ Store in Redis Cache         │
        │ Key: booking:BKG123456       │
        │ TTL: 30 minutes              │
        └────────┬─────────────────────┘
                 │
                 ↓
┌───────────────────────────────────┐
│ BookingController                 │
│ Build ApiResponse<T>              │
│ - status: 200                     │
│ - message: Success                │
│ - data: BookingDetailResponse     │
└────────┬────────────────────────┘
         │
         ↓
    ┌─────────────────────────┐
    │ HTTP 200 JSON Response  │
    │ to Frontend             │
    └─────────────────────────┘
```

---

## III. Database Schema Relationships

```
BOOKINGS (Parent table)
├─ PRIMARY KEY: id
├─ UNIQUE: booking_code
├─ FOREIGN KEY: user_id → users.id (Identity Service)
├─ FOREIGN KEY: departure_id → departures.id (Core Service)
└─ version (Optimistic Locking)

    ↓ One-to-Many
    
PASSENGERS
├─ PRIMARY KEY: id
├─ FOREIGN KEY: booking_id → bookings.id
└─ Data: full_name, dob, gender, id_card, passport

BOOKING_NOTES
├─ PRIMARY KEY: id
├─ FOREIGN KEY: booking_id → bookings.id
└─ Data: content, created_at

CANCELLATIONS (One-to-One)
├─ PRIMARY KEY: id
├─ UNIQUE FOREIGN KEY: booking_id → bookings.id
└─ Data: reason, refund_amount, cancelled_at

FLYWAY_SCHEMA_HISTORY
└─ Tracks migration versions
```

---

## IV. Query Performance Optimization

### Query 1: Get Booking by Code (Fastest)
```sql
SELECT DISTINCT b.*, p.*, bn.*, c.*
FROM bookings b
LEFT JOIN FETCH passengers p ON b.id = p.booking_id
LEFT JOIN FETCH booking_notes bn ON b.id = bn.booking_id
LEFT JOIN FETCH cancellations c ON b.id = c.booking_id
WHERE b.booking_code = 'BKG123456'

-- Indexes used: idx_bookings_code (UNIQUE BTREE)
-- Time: ~5-10ms (with cache: <1ms)
-- Result count: 1 booking + N passengers + N notes + 0-1 cancellation
```

### Query 2: Get Bookings by User (Medium Speed)
```sql
SELECT DISTINCT b.*, p.*
FROM bookings b
LEFT JOIN FETCH passengers p ON b.id = p.booking_id
WHERE b.user_id = 123
ORDER BY b.created_at DESC
LIMIT 10

-- Indexes used: idx_bookings_user_id, idx_bookings_created_at
-- Time: ~20-50ms (depending on dataset)
-- Pagination needed for large result sets
```

### Query 3: Get All Bookings with Filters (Slow - Admin)
```sql
SELECT DISTINCT b.*, p.*
FROM bookings b
LEFT JOIN FETCH passengers p ON b.id = p.booking_id
WHERE b.status = 'PENDING'
  AND b.payment_method = 'CREDIT_CARD'
ORDER BY b.created_at DESC
LIMIT 20

-- Indexes used: idx_bookings_status_payment_created
-- Time: ~50-200ms (depends on data volume)
-- Important: Always use LIMIT/OFFSET for pagination
```

### Cache Hit Rate Target: 70-80%
- Booking lookups by code: ~90% cache hit (booking code rarely changes)
- User bookings: ~60% cache hit (status changes)
- All bookings (admin): ~20% cache hit (frequently filtering)

---

## V. Execution Timeline (6 Days)

### Day 1: DTOs & Repository Layer (2-3 hours)
```
Timeline: 9:00 AM - 12:30 PM

1. Create 6 DTO files (30 min)
   ✓ BookingDetailResponse.java
   ✓ BookingNoteDTO.java
   ✓ CancellationDTO.java
   ✓ BookingSummaryDTO.java
   ✓ PaginatedResponse.java
   ✓ BatchBookingRequest.java

2. Update BookingRepository (20 min)
   ✓ Add 10 custom query methods
   ✓ Use @Query with LEFT JOIN FETCH

3. Create 3 New Repositories (30 min)
   ✓ PassengerRepository
   ✓ BookingNoteRepository
   ✓ CancellationRepository

4. Test Repository Queries (20 min)
   ✓ Write quick JUnit tests
   ✓ Verify no N+1 issues

✓ Deliverable: All DTOs + Repositories ready
```

### Day 2: Service Implementation (4-5 hours)
```
Timeline: 9:00 AM - 2:00 PM

1. Update BookingService Interface (15 min)
   ✓ Add 10 new method signatures
   ✓ Document parameters

2. Implement Core GET Methods (90 min)
   ✓ getBookingByCode()          [15 min]
   ✓ getBookingById()            [10 min]
   ✓ getBookingsByUserId()       [20 min - pagination logic]
   ✓ getAllBookings()            [30 min - filter logic]
   ✓ getPassengersByBookingCode() [10 min]

3. Implement Additional Methods (60 min)
   ✓ getBookingNotesByBookingCode() [10 min]
   ✓ getCancellationByBookingCode() [10 min]
   ✓ getBookingsByIds() - Batch    [15 min]
   ✓ getUserBookingSummary()       [15 min]

4. Add Mapping Methods (30 min)
   ✓ mapToDetailResponse()
   ✓ mapToResponse()
   ✓ mapPassengers(), mapNotes(), etc.

5. Add Logging & Exception Handling (15 min)
   ✓ Add @Transactional(readOnly=true)
   ✓ Add log.info/log.warn statements

✓ Deliverable: Service layer fully implemented
```

### Day 3: Controller & API Endpoints (3-4 hours)
```
Timeline: 9:00 AM - 1:00 PM

1. Add 9 GET Endpoints (90 min)
   ✓ GET /{bookingCode}          [10 min]
   ✓ GET /id/{bookingId}         [10 min]
   ✓ GET /user/{userId}          [15 min - pagination]
   ✓ GET / (all bookings)        [15 min - filters]
   ✓ GET /{bookingCode}/passengers [10 min]
   ✓ GET /{bookingCode}/notes    [10 min]
   ✓ GET /{bookingCode}/cancellation [10 min]
   ✓ POST /batch                 [10 min]
   ✓ GET /user/{userId}/summary  [10 min]

2. Add Request Validation (20 min)
   ✓ @PathVariable validation
   ✓ @RequestParam validation
   ✓ Null checks

3. Add Error Handling (30 min)
   ✓ GlobalExceptionHandler
   ✓ Custom exception mappings
   ✓ Proper HTTP status codes

4. Test Endpoints Manually (30 min)
   ✓ Use Postman/curl
   ✓ Verify response format
   ✓ Test error scenarios

✓ Deliverable: All GET endpoints working
```

### Day 4: Database Indexes & Optimization (2-3 hours)
```
Timeline: 9:00 AM - 12:00 PM

1. Create Flyway Migration Script (20 min)
   ✓ V3__booking_get_api_indexes.sql
   ✓ 10 indexes for performance

2. Run Migration (10 min)
   ✓ Apply to Neon database
   ✓ Verify indexes created

3. Implement Caching (40 min)
   ✓ Configure Redis integration
   ✓ Add cache key strategy
   ✓ Set TTL (30 min)

4. Performance Testing (40 min)
   ✓ Query execution time testing
   ✓ Cache hit rate monitoring
   ✓ Load testing (1000 concurrent requests)

5. HikariCP Configuration (10 min)
   ✓ Connection pool tuning
   ✓ Update application.yml

✓ Deliverable: Database optimized, caching enabled
```

### Day 5: Unit & Integration Tests (4-5 hours)
```
Timeline: 9:00 AM - 2:00 PM

1. Unit Tests for Service Layer (120 min)
   ✓ BookingServiceImpl test suite
   ✓ Mock repository calls
   ✓ Test 10 methods (10 min each)
   ✓ Test error scenarios

2. Integration Tests for Controller (90 min)
   ✓ BookingController test suite
   ✓ Use @SpringBootTest
   ✓ Test all 9 endpoints
   ✓ Test edge cases (pagination, filters)

3. Test Data Fixtures (30 min)
   ✓ Create test Booking entities
   ✓ Create test Passenger data
   ✓ Create test Note data

4. Code Coverage (10 min)
   ✓ Run JaCoCo coverage report
   ✓ Aim for 80%+ coverage

✓ Deliverable: >80% test coverage
```

### Day 6: Documentation & Deployment (3-4 hours)
```
Timeline: 9:00 AM - 1:00 PM

1. Update OpenAPI/Swagger (30 min)
   ✓ Add @Operation annotations
   ✓ Add @ApiResponse annotations
   ✓ Add example payloads

2. Create API Documentation (20 min)
   ✓ README.md updates
   ✓ Endpoint examples
   ✓ Query parameter descriptions

3. Code Review (20 min)
   ✓ Self-review code
   ✓ Check for best practices
   ✓ SonarQube check

4. Create Migration Guide (20 min)
   ✓ Document breaking changes (if any)
   ✓ Document new features
   ✓ Backward compatibility notes

5. Deploy to Staging (20 min)
   ✓ Build JAR
   ✓ Deploy to staging
   ✓ Smoke tests

6. Deploy to Production (10 min)
   ✓ Blue-green deployment
   ✓ Monitor logs
   ✓ Verify all endpoints work

✓ Deliverable: Production-ready code + documentation
```

---

## VI. File Organization After Implementation

```
Booking-service/
├── src/main/java/com/tour/booking/
│   ├── BookingServiceApplication.java
│   ├── controller/
│   │   ├── BookingController.java              [UPDATED - Added 9 GET endpoints]
│   │   └── PassengerController.java
│   ├── service/
│   │   ├── BookingService.java                 [UPDATED - Added 10 methods]
│   │   └── impl/
│   │       ├── BookingServiceImpl.java          [UPDATED - Implemented 10 methods]
│   │       └── PassengerServiceImpl.java
│   ├── repository/
│   │   ├── BookingRepository.java              [UPDATED - Added 10 custom queries]
│   │   ├── PassengerRepository.java            [NEW]
│   │   ├── BookingNoteRepository.java          [NEW]
│   │   └── CancellationRepository.java         [NEW]
│   ├── dto/
│   │   ├── PassengerDTO.java                   [EXISTING]
│   │   ├── response/
│   │   │   ├── ApiResponse.java                [EXISTING]
│   │   │   ├── BookingResponse.java            [EXISTING]
│   │   │   ├── BookingDetailResponse.java      [NEW]
│   │   │   ├── BookingNoteDTO.java             [NEW]
│   │   │   ├── CancellationDTO.java            [NEW]
│   │   │   ├── BookingSummaryDTO.java          [NEW]
│   │   │   ├── PassengerResponseDTO.java       [EXISTING]
│   │   │   └── PaginatedResponse.java          [NEW]
│   │   └── request/
│   │       ├── BookingRequest.java             [EXISTING]
│   │       ├── CancelBookingRequest.java       [EXISTING]
│   │       └── BatchBookingRequest.java        [NEW]
│   ├── entity/
│   │   ├── Booking.java                        [EXISTING]
│   │   ├── Passenger.java                      [EXISTING]
│   │   ├── BookingNote.java                    [EXISTING]
│   │   └── Cancellation.java                   [EXISTING]
│   ├── config/
│   │   ├── CacheConfig.java                    [NEW - Redis caching]
│   │   ├── ModelMapperConfig.java              [EXISTING]
│   │   └── OpenApiConfig.java                  [EXISTING]
│   └── exception/
│       ├── GlobalExceptionHandler.java         [UPDATED]
│       ├── ResourceNotFoundException.java      [EXISTING]
│       ├── BusinessException.java              [EXISTING]
│       └── InvalidDataException.java           [EXISTING]
│
├── src/test/java/com/tour/booking/
│   ├── service/
│   │   └── BookingServiceImplTest.java         [NEW]
│   └── controller/
│       └── BookingControllerIntegrationTest.java [NEW]
│
├── src/main/resources/
│   ├── application.yml                         [EXISTING]
│   ├── application-dev.yml                     [EXISTING]
│   └── db/migration/
│       ├── V1__init.sql                        [EXISTING]
│       ├── V2__seed_booking_data.sql           [EXISTING]
│       └── V3__booking_get_api_indexes.sql     [NEW]
│
├── BOOKING_SERVICE_GET_API_IMPLEMENTATION.md   [NEW - Full guide]
├── BOOKING_SERVICE_CODE_TEMPLATES.md           [NEW - Code templates]
└── pom.xml                                     [EXISTING]
```

---

## VII. Dependencies & Versions

### Current Stack (To Verify)
```xml
<!-- Spring Boot -->
<spring-boot.version>3.x.x</spring-boot.version>

<!-- Data Access -->
<spring-boot-starter-data-jpa>
<spring-boot-starter-data-rest>
<flyway-core>9.x.x</flyway-core>

<!-- Database Driver -->
<postgresql>42.x.x</postgresql>

<!-- Caching -->
<redisson-spring-boot-starter>3.x.x</redisson-spring-boot-starter>

<!-- API Documentation -->
<springdoc-openapi-starter-webmvc-ui>2.x.x</springdoc-openapi-starter-webmvc-ui>

<!-- Testing -->
<spring-boot-starter-test>
<spring-boot-testcontainers>

<!-- Serialization -->
<jackson-databind>
</jackson-databind>

<!-- Lombok -->
<lombok>1.x.x</lombok>

<!-- Logging -->
<spring-boot-starter-logging> <!-- Built-in with spring-boot-starter -->
```

### Recommended additions (If Not Present)
```xml
<!-- For pagination & filtering -->
<spring-data-commons>3.x.x</spring-data-commons>

<!-- For better API documentation -->
<springdoc-openapi-starter-webmvc-ui>2.x.x</springdoc-openapi-starter-webmvc-ui>
```

---

## VIII. Key Metrics & Monitoring

### After Implementation, Monitor:
```
1. Query Performance
   - Avg GET by code response time: < 50ms (with cache: < 10ms)
   - Avg GET user bookings time: < 200ms
   - P99 response time: < 500ms

2. Cache Performance
   - Cache hit rate target: 70-80%
   - Cache eviction rate: < 5%
   - Cache memory usage: < 500MB

3. Database Performance
   - Connection pool utilization: 50-70%
   - Slow query log: 0 queries > 1000ms
   - Index usage: All queries should use indexes

4. API Usage
   - Requests per second: Monitor growth
   - Error rate: < 1%
   - 404 rate: < 2% (indicates invalid bookings)

5. Application Metrics
   - Thread pool utilization: < 80%
   - GC pause time: < 100ms
   - Memory usage: Stable growth
```

---

## IX. Rollback Plan (If Issues)

### If Something Goes Wrong:
```
1. Database Indexes Problem
   - Rollback: Remove V3 migration
   - Command: DROP INDEX idx_* CASCADE;
   - Redeploy previous version

2. API Response Format Issue
   - Rollback: Revert controller & service commits
   - Keep database changes (indexes safe)
   - Increment to V4 migration

3. Cache Issue
   - Clear all cache keys: redis-cli FLUSHDB
   - Disable Redis integration temporarily
   - Re-enable after fix

4. Performance Regression
   - Roll back to V2 database state
   - Check slow query logs
   - Re-optimize indexes
```

---

## X. Success Criteria Checklist

- [ ] All 9 GET endpoints implemented and working
- [ ] No N+1 query problems
- [ ] Pagination working correctly (limit, offset, totalPages)
- [ ] Filtering working (status, paymentMethod, etc.)
- [ ] Error handling returns proper HTTP status codes
- [ ] Database indexes created and in use
- [ ] Caching enabled with 70%+ hit rate
- [ ] Unit tests: 80%+ coverage
- [ ] Integration tests: All endpoints tested
- [ ] API documentation complete
- [ ] Load testing passed (1000 concurrent requests)
- [ ] Deployed to staging & production successfully

