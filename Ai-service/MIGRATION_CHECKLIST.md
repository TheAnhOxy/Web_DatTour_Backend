# Migration Checklist: Mock → Live LLM Mode

## 📊 Đánh giá hiện tại (Mock Mode)

- ✅ **Intent Understanding**: 60% (vì regex keyword matching)
- ✅ **Entity Extraction**: 50% (nhầm tháng/ngân sách, mã booking)
- ✅ **Context Memory**: 70% (nhớ destination nhưng đôi khi rớt)
- ✅ **Complaint Handling**: 40% (chưa nhận diện được sentiment âm)
- ✅ **Tool Calling**: 80% (hoạt động nhưng còn format mong manh)

---

## 🚀 Checklist: Chuyển sang Live LLM

### **Phase 1: Chuẩn bị Môi trường (30 phút)**

- [ ] Kiểm tra Redis đang chạy
  ```bash
  redis-cli ping
  # Kỳ vọng: PONG
  ```

- [ ] Kiểm tra API Key Gemini trong `.env`
  ```bash
  cat .env | grep GEMINI_API_KEY
  # Kỳ vọng: AIzaSyD... (không trống)
  ```

- [ ] Kiểm tra kết nối Internet (Gemini API cần kết nối)
  ```bash
  ping google.com
  ```

- [ ] (Tùy chọn) Kiểm tra Kafka nếu muốn test escalation
  ```bash
  docker ps | grep kafka
  ```

---

### **Phase 2: Chuyển Cấu Hình (5 phút)**

**Trước:**
```bash
$env:AI_MODE="mock"
$env:REDIS_URL="redis://localhost:6379/0"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:29092"
```

**Sau:**
```bash
# Xóa bỏ AI_MODE hoặc đặt = "auto"
$env:AI_MODE="auto"
$env:REDIS_URL="redis://localhost:6379/0"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:29092"
```

**Hoặc chỉnh trực tiếp `.env`:**
```env
# GEMINI_API_KEY=AIzaSyDTx5cNicqNPsqsx5NtwCLEYy3x2rFWEug
AI_MODE=auto          # Thay từ "mock" thành "auto"
```

---

### **Phase 3: Khởi Động Service (10 phút)**

```bash
# 1. Vào thư mục Ai-service
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE\Ai-service

# 2. Kích hoạt virtual environment
.\venv\Scripts\Activate.ps1

# 3. Khởi động service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Logs mong đợi:**
```
INFO:     Started server process [12345]
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Connected to Redis!
INFO:     Connected to Kafka! (tùy chọn)
```

---

### **Phase 4: Test Các Trường Hợp Lỗi (15 phút)**

Dùng Postman collection, chạy các test case và kiểm tra:

#### **Test Case B - Intent Understanding**

**Trước (Mock):**
```json
{
  "intent": "greeting",  ❌ Sai, phải là tour_search
  "sentiment": "neutral"
}
```

**Sau (LLM):**
```json
{
  "intent": "tour_search",  ✅ Đúng
  "sentiment": "neutral"
}
```

#### **Test Case F - Complaint Handling**

**Trước (Mock):**
```json
{
  "intent": "tour_search",  ❌ Sai, phải là complaint
  "sentiment": "neutral",  ❌ Sai, phải là negative
  "requires_human_support": false  ❌ Sai, phải là true
}
```

**Sau (LLM):**
```json
{
  "intent": "complaint",  ✅ Đúng
  "sentiment": "negative",  ✅ Đúng
  "requires_human_support": true  ✅ Đúng
}
```

---

### **Phase 5: Xử lý Lỗi Phổ Biến**

| Lỗi | Nguyên nhân | Cách khắc phục |
|-----|-----------|---------|
| `Error calling Gemini API: connect timeout` | Internet chậm hoặc API Key sai | Kiểm tra kết nối, test API key |
| `ValueError: API Key is missing` | `.env` không có API key | Thêm API key vào `.env` |
| `ConnectionRefusedError: Redis` | Redis không chạy | Chạy `redis-server` |
| `Gemini trả lời sai intent` | Prompt Gemini cần cải thiện | Xem section "Prompt Tuning" |
| `Timeout 5s khi gọi Gemini` | Request quá dài hoặc API slow | Giảm lịch sử chat, hoặc bỏ qua |

---

### **Phase 6: Tuning & Optimization (1h)**

Nếu LLM vẫn trả lời chưa chính xác:

**1. Kiểm tra Prompt Gemini**
Mở [gemini_service.py](gemini_service.py) và xem phần `async def extract_entities` và `async def chat_with_travel_assistant`. Nếu LLM vẫn nhầm, hãy:
- Thêm ví dụ rõ ràng hơn trong prompt
- Yêu cầu LLM giải thích trước khi quyết định
- Giảm số lượng tour context để LLM dễ tập trung

**2. Test với mock data có sẵn**
Mở [mock_travel_knowledge.py](mock_travel_knowledge.py), xem mảng `MOCK_TOURS` và đảm bảo có đủ các loại tour (biển, núi, quốc tế, tiết kiệm, cao cấp).

**3. Cải thiện extracted_entities**
Nếu LLM vẫn nhầm budget/tháng:
```python
# Thêm vào prompt
- QUAN TRỌNG: "Tháng 11" là thời gian, KHÔNG PHẢI ngân sách.
- "5 triệu", "7tr" mới là ngân sách.
```

**4. Test từng nhóm Postman**
Nhóm nào fail thì note lại và submit issue cho Gemini tuning.

---

## 📈 Tiến trình chuyển đổi

| Phase | Task | Thời gian | Status |
|-------|------|----------|--------|
| 1 | Chuẩn bị môi trường | 30 phút | [ ] |
| 2 | Chuyển cấu hình | 5 phút | [ ] |
| 3 | Khởi động service | 10 phút | [ ] |
| 4 | Test case | 15 phút | [ ] |
| 5 | Xử lý lỗi | 15 phút | [ ] |
| 6 | Tuning | 1 giờ | [ ] |

**Tổng cộng: ~2 giờ** từ mock → stable live LLM

---

## ✅ Tiêu chí "Ready for Production"

Khi tất cả những điều dưới đây thỏa mãn, bạn có thể chuyển sang dữ liệu thật từ core-service:

- [ ] **Intent Detection**: ≥ 90% chính xác (test với 20 case random)
- [ ] **Entity Extraction**: ≥ 85% (không nhầm tháng/ngân sách)
- [ ] **Context Memory**: ≥ 95% (3 turn liên tiếp đều nhớ destination)
- [ ] **Complaint Handling**: 100% (tất cả negative sentiment → requires_human_support = true)
- [ ] **Tool Calling**: 100% (all tool calls format đúng)
- [ ] **No Hallucination**: 100% (không bịa tour không có trong context)
- [ ] **Response Time**: < 2s (kể cả gọi Gemini)
- [ ] **Redis Uptime**: 99.9% (không mất chat history)

---

## 🔄 Phục hồi nếu cần quay lại Mock Mode

Nếu LLM làm hỏng gì, quay lại mock mode ngay:

```bash
# 1. Dừng service (Ctrl+C)
# 2. Chuyển AI_MODE
$env:AI_MODE="mock"

# 3. Khởi động lại
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

---

## 📞 Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra logs trong terminal
2. Xem section "Xử lý Lỗi Phổ Biến"
3. Test 1-2 case Postman xem có response không
4. Nếu vẫn lỗi, quay lại mock mode để debug

---

## 📝 Ghi chú

- **Không nên** bật live mode khi chưa chuẩn bị đủ (Redis, API key, internet)
- **Nên** test mock mode kỹ trước khi chuyển live
- **Nên** giữ bản mock mode để so sánh kết quả
- **Lưu ý** Gemini API key có thể hết quota sau ~100 request/phút
