# Hướng dẫn chạy AI-Service trên Local

## 1️⃣ Yêu cầu phụ thuộc

### Tối thiểu (Mock mode - không cần Kafka):
- **Python 3.9+**
- **Redis** (để lưu chat history)

### Đầy đủ (Live LLM mode):
- **Python 3.9+**
- **Redis** (Redis 7+)
- **Kafka** (với Zookeeper)
- **MongoDB** (tùy chọn, hiện tại chưa dùng mạnh)
- **Google Gemini API Key** (đã có trong `.env`)

---

## 2️⃣ Cài đặt môi trường nhanh

### **Bước 1: Khởi động Redis (bắt buộc)**

```bash
# Nếu dùng Docker
docker run --name redis -d -p 6379:6379 redis:7-alpine

# Hoặc chạy local (nếu cài sẵn Redis)
redis-server
```

**Kiểm tra Redis chạy:**
```bash
redis-cli ping
# Kỳ vọng: PONG
```

---

### **Bước 2: Khởi động Kafka (nếu cần escalation support)**

```bash
# Chạy toàn bộ docker-compose từ thư mục BE
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE
docker-compose up -d redis kafka zookeeper

# Kiểm tra Kafka
docker ps | grep kafka
```

---

### **Bước 3: Cài đặt Python dependencies**

```bash
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE\Ai-service

# Tạo virtual environment
python -m venv venv
.\venv\Scripts\Activate.ps1

# Cài đặt requirements
pip install -r requirements.txt
```

---

## 3️⃣ Chạy AI-Service

### **Option A: Chế độ Mock (nên bắt đầu từ đây)**

```bash
# Vào thư mục Ai-service
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE\Ai-service

# Bật virtual environment
.\venv\Scripts\Activate.ps1

# Set environment variables
$env:AI_MODE="mock"
$env:REDIS_URL="redis://localhost:6379/0"

# Chạy server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Kỳ vọng:**
```
INFO:     Uvicorn running on http://0.0.0.0:8000
```

**Test ngay:**
```bash
curl -X POST http://localhost:8000/api/chat/ \
  -H "Content-Type: application/json" \
  -d '{"session_id":"test-1", "message":"Tôi muốn đi Đà Lạt"}'
```

---

### **Option B: Chế độ Live (LLM + Redis + Kafka)**

```bash
# Vào thư mục Ai-service
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE\Ai-service

# Bật virtual environment
.\venv\Scripts\Activate.ps1

# Set environment variables (không set AI_MODE, mặc định = auto)
$env:REDIS_URL="redis://localhost:6379/0"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:29092"

# Chạy server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Kiểm tra logs:**
- Nếu thấy `Connected to Redis!` → Redis OK
- Nếu thấy `Connected to Kafka!` → Kafka OK
- Nếu thấy error, có thể bỏ qua (service vẫn chạy được)

---

## 4️⃣ Endpoint chính

### **Endpoint chat:**
```
POST http://localhost:8000/api/chat/

Request body:
{
  "session_id": "user-123",  // Session ID để giữ context
  "message": "Tôi muốn đi Đà Lạt 3 ngày dưới 5 triệu",
  "history": []  // Tùy chọn: lịch sử chat trước
}

Response:
{
  "reply": "...",
  "intent": "tour_search",
  "sentiment": "neutral",
  "suggested_tours": [...],
  "suggested_questions": [...],
  "extracted_entities": {...},
  "tool_calls": [],
  "requires_human_support": false,
  "metadata": {...}
}
```

### **Endpoint health check:**
```
GET http://localhost:8000/health
Response: {"status": "ok", "service": "ai-service"}
```

---

## 5️⃣ Các môi trường trong `.env`

```env
# LLM Backend
GEMINI_API_KEY=AIzaSyDTx5cNicqNPsqsx5NtwCLEYy3x2rFWEug
AI_MODE=mock                           # "mock", "auto", "test", "demo"

# Database
MONGODB_URL=mongodb+srv://tuananh123:tuananh123@cluster0.foryjm4.mongodb.net/?appName=Cluster0
DATABASE_NAME=webtour_ai

# Cache & Event
REDIS_URL=redis://localhost:6379/0
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
```

### **Cách thay đổi mode:**

```bash
# Mock mode (chỉ dùng regex/keyword)
$env:AI_MODE="mock"

# Live LLM mode (dùng Gemini)
$env:AI_MODE="auto"
# hoặc bỏ qua, mặc định sẽ là "auto"

# Khởi động lại service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

---

## 6️⃣ Xử lý lỗi phổ biến

### **Lỗi: `ConnectionRefusedError: [Errno 10061]` khi Redis**
```
❌ [Errno 10061] No connection could be made because the target machine actively refused it
```
→ Redis chưa chạy. Khởi động Redis trước.

### **Lỗi: Timeout khi gọi Gemini API**
```
❌ Error calling Gemini API: <urlopen error ...>
```
→ Kiểm tra kết nối Internet, API Key có hợp lệ không.

### **Lỗi: Kafka không kết nối (nhưng không cần thiết cho chat cơ bản)**
```
❌ Error connecting to Kafka: ...
```
→ Bình thường khi chạy mock mode. Kafka chỉ cần khi user phàn nàn (escalation).

---

## 7️⃣ Quickstart cho Postman

1. **Khởi động service:**
   ```bash
   $env:AI_MODE="mock"
   $env:REDIS_URL="redis://localhost:6379/0"
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

2. **Import Postman collection** (xem file `POSTMAN_COLLECTION.json`)

3. **Chạy test từng nhóm** (A, B, C, ..., J)

4. **Xem kết quả** trong Response body:
   - `intent`, `sentiment`, `extracted_entities`, `suggested_tours`

---

## 8️⃣ Chuyển từ Mock → Live LLM

Sau khi test xong mock mode, chuyển sang LLM thật:

```bash
# 1. Đảm bảo Redis chạy
redis-cli ping

# 2. Thay đổi AI_MODE
$env:AI_MODE="auto"

# 3. (Tùy chọn) Bật Kafka nếu muốn test escalation
docker-compose up -d kafka zookeeper

# 4. Khởi động lại service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 5. Test lại với Postman collection
```

Bây giờ AI sẽ dùng **Google Gemini** thay vì regex rules.

---

## 📝 Tóm tắt

| Mode | Redis | Kafka | Gemini | Tốc độ | Chính xác |
|------|-------|-------|--------|-------|----------|
| **Mock** | ✓ | - | - | Nhanh | 60% |
| **Live (Auto)** | ✓ | - | ✓ | Chậm | 95%+ |
| **Production** | ✓ | ✓ | ✓ | Trung bình | 95%+ |

Khuyến cáo: **Bắt đầu bằng Mock mode, rồi chuyển Live khi logic ổn định.**
