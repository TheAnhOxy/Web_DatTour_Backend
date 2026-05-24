# Booking Service GET API - Testing Guide

## Example API Calls

### 1. Get Booking by Code
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/BK123456789" \
  -H "Content-Type: application/json"
```
Response:
```json
{
  "status": 200,
  "message": "Lấy chi tiết đơn hàng thành công",
  "data": {
    "bookingId": 1,
    "bookingCode": "BK123456789",
    "userId": 100,
    "departureId": 50,
    "totalAmount": 2500000,
    "paidAmount": 0,
    "status": "PENDING",
    "paymentMethod": "VNPAY",
    "paymentDueAt": "2026-05-24T14:30:00",
    "contactName": "Nguyen Van A",
    "contactEmail": "a@example.com",
    "contactPhone": "0912345678",
    "passengers": [
      {
        "fullName": "Nguyen Van A",
        "dob": "1990-01-01",
        "gender": "MALE",
        "ageGroup": "ADULT",
        "idCardNumber": "123456789"
      }
    ],
    "bookingNotes": [],
    "cancellation": null,
    "createdAt": "2026-05-24T12:30:00"
  }
}
```

### 2. Get Booking by ID
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/id/1" \
  -H "Content-Type: application/json"
```

### 3. Get User's Bookings (with Pagination)
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/user/100?status=PENDING&page=0&limit=10" \
  -H "Content-Type: application/json"
```
Response:
```json
{
  "status": 200,
  "message": "Lấy danh sách đơn hàng thành công",
  "data": {
    "data": [
      { /* booking objects */ }
    ],
    "totalElements": 25,
    "currentPage": 0,
    "pageSize": 10,
    "totalPages": 3,
    "hasNext": true
  }
}
```

### 4. Get All Bookings (Admin - with Filters)
```bash
curl -X GET "http://localhost:8080/api/v1/bookings?status=CONFIRMED&paymentMethod=VNPAY&page=0&limit=20" \
  -H "Content-Type: application/json"
```

### 5. Get Passengers for Booking
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/BK123456789/passengers" \
  -H "Content-Type: application/json"
```
Response:
```json
{
  "status": 200,
  "message": "Lấy danh sách hành khách thành công",
  "data": [
    {
      "fullName": "Nguyen Van A",
      "dob": "1990-01-01",
      "gender": "MALE",
      "ageGroup": "ADULT",
      "idCardNumber": "123456789"
    }
  ]
}
```

### 6. Get Booking Notes
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/BK123456789/notes" \
  -H "Content-Type: application/json"
```

### 7. Get Cancellation Info
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/BK123456789/cancellation" \
  -H "Content-Type: application/json"
```

### 8. Batch Get Bookings
```bash
curl -X POST "http://localhost:8080/api/v1/bookings/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingIds": [1, 2, 3, 4, 5]
  }'
```
Response:
```json
{
  "status": 200,
  "message": "Lấy danh sách đơn hàng thành công",
  "data": {
    "1": { /* booking details */ },
    "2": { /* booking details */ },
    "3": { /* booking details */ }
  }
}
```

### 9. Get User Statistics Summary
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/user/100/summary" \
  -H "Content-Type: application/json"
```
Response:
```json
{
  "status": 200,
  "message": "Lấy tóm tắt đơn hàng thành công",
  "data": {
    "totalBookings": 10,
    "totalAmount": 25000000,
    "totalPaidAmount": 15000000,
    "byStatus": {
      "PENDING": 2,
      "CONFIRMED": 5,
      "PAID": 3,
      "COMPLETED": 0,
      "CANCELLED": 0
    }
  }
}
```

## Testing Checklist

### Basic Functionality
- [ ] Get single booking by code
- [ ] Get single booking by ID
- [ ] Get non-existent booking (should return 404)
- [ ] Get user's bookings (empty result)
- [ ] Get user's bookings with pagination
- [ ] Filter by status
- [ ] Filter by payment method
- [ ] Get passengers list
- [ ] Get booking notes
- [ ] Get cancellation info
- [ ] Batch get bookings
- [ ] Get user summary

### Pagination Tests
- [ ] page=0, limit=10 (first page)
- [ ] page=1, limit=10 (second page)
- [ ] page beyond total pages
- [ ] hasNext flag correctness
- [ ] totalPages calculation

### Filter Tests
- [ ] Filter by PENDING status
- [ ] Filter by CONFIRMED status
- [ ] Filter by VNPAY payment method
- [ ] Filter by CREDIT_CARD payment method
- [ ] Combine status + payment method filters

### Performance Tests
- [ ] Response time < 200ms for single booking
- [ ] Response time < 500ms for paginated list
- [ ] Verify N+1 queries are eliminated
- [ ] Test with large datasets

### Error Handling
- [ ] Invalid booking code (404)
- [ ] Invalid user ID (empty list or 404)
- [ ] Invalid pagination parameters
- [ ] Missing required query parameters

## Data Validation Notes

### Booking Statuses
- PENDING - Waiting for payment
- CONFIRMED - Confirmed but not yet paid
- PAID - Payment received
- COMPLETED - Tour completed
- CANCELLED - Cancelled by user

### Payment Methods
- CREDIT_CARD
- VNPAY
- MOMO
- BANK_TRANSFER
- CASH_OFFICE

### Age Groups
- ADULT
- CHILD_10_14
- CHILD_4_9
- BABY

