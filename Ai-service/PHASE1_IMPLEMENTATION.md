# 🔥 PHASE 1: ARCHITECTURE FIX BLUEPRINT

## Current State: "Response-Driven Router"
```
Keyword Match → Intent → Template Response
```

## Target State: "Stateful Reasoning Assistant"
```
Normalize → Negative Detection → Intent Classification (LLM) 
→ Entity Extraction → Session State → Tool Routing → Ranking → Response
```

---

## PHASE 1 Components to Build/Fix

### 1. Enhanced Entity Extractor
**File**: `app/services/entity_extractor.py` (NEW)

**Current Problem**:
```python
# BUG: regex runs without priority
re.findall(r"\d+[MmTt]", msg)  # Matches "BK123" → "123" → $123M ❌
```

**New Logic - Priority Order**:
```
1. BOOKING_ID: r"\bBK\d{3,5}\b" → "BK123" → booking_id
2. MONTH: r"(tháng|month)\s*(\d{1,2})" → "tháng 11" → month
3. BUDGET: r"(\d+\.?\d*)\s*[MmTt]" BUT exclude if already booking_id or month
4. DESTINATION: ["Đà Lạt", "Đà Nẵng", "Nhật Bản", ...]
5. DAYS: r"(\d+)\s*(ngày|day|n)" → "3 ngày" → days
6. GROUP: ["gia đình", "bạn", "người yêu", ...]
```

**Implementation**:
```python
class EnhancedEntityExtractor:
    def extract(self, msg: str) -> dict:
        # Step 1: Normalize
        msg = self.normalize(msg)  # lowercase, remove diacritics option
        
        # Step 2: Priority extraction (order matters!)
        result = {}
        
        # 2a: Booking ID FIRST
        booking_match = re.search(r"\bBK\d{3,5}\b", msg)
        if booking_match:
            result["booking_id"] = booking_match.group()
            msg = msg.replace(result["booking_id"], "")  # Remove from further processing
        
        # 2b: Month SECOND
        month_match = re.search(r"(tháng|month|t)\s*(\d{1,2})", msg)
        if month_match:
            result["month"] = int(month_match.group(2))
            msg = msg.replace(month_match.group(), "")  # Remove
        
        # 2c: Budget THIRD (now safe from booking_id and month)
        budget_match = re.search(r"(\d+\.?\d*)\s*[MmTt](?:iệu|r|riệu)?", msg)
        if budget_match:
            result["budget"] = float(budget_match.group(1)) * 1_000_000
            msg = msg.replace(budget_match.group(), "")
        
        # 2d: Destination
        for dest in DESTINATIONS:
            if dest.lower() in msg.lower():
                result["destination"] = dest
                break
        
        # 2e: Days
        days_match = re.search(r"(\d+)\s*(ngày|day|n|n2d|3n2d)", msg)
        if days_match:
            result["days"] = int(days_match.group(1))
        
        # 2f: Group
        for group in TRAVEL_GROUPS:
            if group in msg:
                result["group"] = group
                break
        
        return result
```

---

### 2. Negative Word Detection (EARLY & PRIORITY)
**File**: `app/services/negative_detector.py` (NEW)

**Current Problem**: 
- Negative detection happens AFTER intent classification
- AI already decided intent=tour_search before checking "tệ" ❌

**New Logic**:
```python
NEGATIVE_WORDS = {
    "tệ": 2.0,
    "thất vọng": 2.0,
    "khô chịu": 2.0,
    "bực": 2.0,
    "lừa": 2.5,
    "refund": 2.5,
    "hoàn tiền": 2.5,
    "chậm": 1.5,
    "hỏng": 1.5,
    "cáo buộc": 2.5,
}

class NegativeDetector:
    def detect(self, msg: str) -> dict:
        msg_lower = msg.lower()
        
        negative_score = 0
        detected_words = []
        
        for word, score in NEGATIVE_WORDS.items():
            if word in msg_lower:
                negative_score += score
                detected_words.append(word)
        
        return {
            "has_negative_sentiment": negative_score >= 1.5,
            "negative_score": negative_score,
            "detected_words": detected_words,
            "sentiment": "negative" if negative_score >= 1.5 else "neutral"
        }
```

---

### 3. Proper Intent Classifier (Gemini)
**File**: `app/services/intent_classifier.py` (NEW)

**Current Problem**:
- Keyword matching fallback triggers greeting for every message
- No Gemini-based classification before tool dispatch

**New Logic**:
```python
class IntentClassifier:
    async def classify(self, msg: str, entities: dict, 
                       negative_detector_result: dict) -> dict:
        
        # EARLY RETURN: If negative sentiment detected
        if negative_detector_result["has_negative_sentiment"]:
            return {
                "intent": "complaint",
                "confidence": 1.0,
                "reason": f"Negative words detected: {negative_detector_result['detected_words']}"
            }
        
        # Otherwise: Use Gemini
        prompt = f"""
Classify the following user message into ONE of these intents:
- greeting: Chào hỏi, giới thiệu bản thân (ví dụ: "Xin chào", "Bạn là ai?")
- casual_chat: Nói chuyện bình thường, không liên quan du lịch (ví dụ: "Bạn thích đi đâu?", "Hôm nay thời tiết sao?")
- tour_search: Tìm tour cụ thể hoặc lọc theo tiêu chí (ví dụ: "Tour Đà Lạt giá bao nhiêu?", "Tháng 11 nên đi đâu?")
- recommendation: Yêu cầu AI recommend dựa trên nhu cầu (ví dụ: "Nên đi tour nào?", "Có gì phù hợp với gia đình không?")
- comparison: So sánh các tour (ví dụ: "Tour A hay B đáng hơn?", "So sánh tour này")
- booking_support: Hỗ trợ booking, thanh toán, kiểm tra trạng thái (ví dụ: "Booking BK123 ở đâu?", "Hủy đơn")
- complaint: Phàn nàn, phản hồi tiêu cực (đã được detect ở trên)
- other: Không vào danh mục nào

User Message: "{msg}"
Extracted Entities: {entities}

RULES:
- "Tour Đà Lạt giá bao nhiêu?" → tour_search (NOT greeting)
- "Bạn là ai?" → casual_chat hoặc greeting (NOT tour_search)
- "Đi đâu đẹp?" → recommendation (NOT tour_search)
- "Tour A hay B?" → comparison (NOT tour_search)

Output JSON:
{{
    "intent": "<one of above>",
    "confidence": <0.0-1.0>,
    "reasoning": "<explain why>"
}}
"""
        
        try:
            response = await self.gemini_client.generate(prompt)
            return json.loads(response)
        except Exception as e:
            # Fallback to keyword-based if Gemini fails
            return self._keyword_fallback(msg)
    
    def _keyword_fallback(self, msg: str) -> dict:
        """Fallback if Gemini unavailable - MUST NOT be primary logic"""
        msg_lower = msg.lower()
        
        if any(w in msg_lower for w in ["chào", "hello", "hi"]):
            return {"intent": "greeting", "confidence": 0.6}
        elif any(w in msg_lower for w in ["so sánh", "compare", "hay hơn"]):
            return {"intent": "comparison", "confidence": 0.7}
        elif any(w in msg_lower for w in ["giá", "bao nhiêu", "booking", "BK"]):
            return {"intent": "tour_search", "confidence": 0.7}
        else:
            return {"intent": "other", "confidence": 0.5}
```

---

### 4. Expanded Session State Manager
**File**: `app/services/session_manager.py` (ENHANCE)

**Current State Schema** (partial):
```json
{
    "chat_history": [...]
}
```

**New State Schema** (complete):
```python
class ConversationState(BaseModel):
    session_id: str
    
    # Context from current conversation
    last_intent: str = None
    last_destination: str = None
    last_budget: float = None
    last_days: int = None
    last_group: str = None
    last_booking_id: str = None
    
    # Last search results for multi-turn reference
    last_tour_ids: List[str] = []
    last_tour_results: List[dict] = []
    last_filters: dict = {}
    
    # Conversation mode
    conversation_mode: str = "DISCOVERY"  # DISCOVERY | BOOKING_SUPPORT | COMPLAINT | CASUAL
    
    # Chat history
    chat_history: List[dict] = []
    
    # Timestamps
    created_at: datetime
    updated_at: datetime
    last_active: datetime

class SessionManager:
    async def load_state(self, session_id: str) -> ConversationState:
        """Load full conversation state from Redis"""
        data = await self.redis.get(f"session:{session_id}")
        if not data:
            return ConversationState(session_id=session_id, created_at=now(), updated_at=now())
        return ConversationState(**json.loads(data))
    
    async def save_state(self, state: ConversationState):
        """Save full state, keyed by session_id"""
        await self.redis.set(
            f"session:{state.session_id}",
            state.json(),
            ex=86400  # 24h TTL
        )
    
    async def update_context(self, session_id: str, 
                            intent: str, 
                            entities: dict,
                            tour_results: List[dict] = None):
        """Update conversation context after each turn"""
        state = await self.load_state(session_id)
        
        state.last_intent = intent
        state.last_destination = entities.get("destination")
        state.last_budget = entities.get("budget")
        state.last_days = entities.get("days")
        state.last_group = entities.get("group")
        state.last_booking_id = entities.get("booking_id")
        
        if tour_results:
            state.last_tour_ids = [t["id"] for t in tour_results]
            state.last_tour_results = tour_results
        
        state.updated_at = datetime.now()
        state.last_active = datetime.now()
        
        await self.save_state(state)
        return state
```

---

### 5. Conversation Mode FSM
**File**: `app/services/conversation_fsm.py` (NEW)

```python
class ConversationMode(str, Enum):
    DISCOVERY = "DISCOVERY"           # Exploring options
    BOOKING_SUPPORT = "BOOKING_SUPPORT"  # Processing booking
    COMPLAINT = "COMPLAINT"            # Handling issues
    CASUAL = "CASUAL"                  # General chat

class ConversationFSM:
    # State machine rules
    TRANSITIONS = {
        "DISCOVERY": {
            "booking_support": "BOOKING_SUPPORT",
            "complaint": "COMPLAINT",
            "casual": "CASUAL",
        },
        "BOOKING_SUPPORT": {
            "complaint": "COMPLAINT",
            "discovery": "DISCOVERY",
        },
        "COMPLAINT": {
            "booking_support": "BOOKING_SUPPORT",
            "discovery": "DISCOVERY",
        },
        "CASUAL": {
            "discovery": "DISCOVERY",
            "booking_support": "BOOKING_SUPPORT",
        }
    }
    
    async def determine_mode(self, intent: str, current_mode: str) -> str:
        """Determine next conversation mode based on intent"""
        
        # Intent → mode mapping
        intent_to_mode = {
            "tour_search": "DISCOVERY",
            "recommendation": "DISCOVERY",
            "comparison": "DISCOVERY",
            "booking_support": "BOOKING_SUPPORT",
            "complaint": "COMPLAINT",
            "casual_chat": "CASUAL",
            "greeting": "CASUAL",
        }
        
        new_mode = intent_to_mode.get(intent, current_mode)
        
        # Check if transition is allowed
        if new_mode in self.TRANSITIONS.get(current_mode, {}):
            return new_mode
        
        # If not allowed, stay in current mode
        return current_mode
    
    def get_system_prompt_adjustment(self, mode: str) -> str:
        """Adjust response tone based on conversation mode"""
        
        adjustments = {
            "DISCOVERY": "Hãy gợi ý tour phù hợp, giải thích tại sao.",
            "BOOKING_SUPPORT": "Hãy hỗ trợ booking, kiểm tra trạng thái, hướng dẫn thanh toán.",
            "COMPLAINT": "Hãy lắng nghe, ghi nhận phản hồi, đề xuất giải pháp, escalate nếu cần.",
            "CASUAL": "Hãy trò chuyện tự nhiên, thân thiện, vui vẻ."
        }
        
        return adjustments.get(mode, "")
```

---

## Implementation Order (STRICT SEQUENCE)

1. **EntityExtractor** ← Must be fixed first (dependency for everything)
2. **NegativeDetector** ← Must be early in pipeline
3. **IntentClassifier** ← Uses entities + negative result
4. **SessionManager** ← Stores enhanced state
5. **ConversationFSM** ← Uses intent to determine mode

---

## Modified Chat Flow (NEW)

```python
@app.post("/api/chat/")
async def chat(request: ChatRequest):
    # 1. NORMALIZE
    normalized_msg = normalize(request.message)
    
    # 2. NEGATIVE DETECTION (EARLY)
    negative_result = await negative_detector.detect(normalized_msg)
    
    # 3. ENTITY EXTRACTION (NEW LOGIC)
    entities = await entity_extractor.extract(normalized_msg)
    
    # 4. INTENT CLASSIFICATION (LLM)
    intent_result = await intent_classifier.classify(
        normalized_msg, 
        entities, 
        negative_result
    )
    intent = intent_result["intent"]
    
    # 5. LOAD SESSION STATE (ENHANCED)
    state = await session_manager.load_state(request.session_id)
    
    # 6. DETERMINE CONVERSATION MODE
    mode = await fsm.determine_mode(intent, state.conversation_mode)
    
    # 7. ROUTE BASED ON INTENT & MODE
    if intent == "complaint":
        response = await handle_complaint(entities, negative_result)
    elif intent == "booking_support":
        response = await handle_booking(entities, state)
    elif intent == "comparison":
        response = await handle_comparison(entities, state)
    elif intent == "recommendation":
        response = await handle_recommendation(entities, state)
    elif intent == "tour_search":
        response = await handle_tour_search(entities, state)
    else:
        response = await handle_casual(normalized_msg, state)
    
    # 8. UPDATE SESSION STATE (NEW CONTEXT)
    await session_manager.update_context(
        request.session_id,
        intent,
        entities,
        response.get("suggested_tours", [])
    )
    
    # 9. RETURN RESPONSE
    return response
```

---

## Expected Test Results After PHASE 1

### Before PHASE 1
```
"Tour Đà Lạt giá bao nhiêu?" → intent=greeting ❌
"Tháng 11 nên đi đâu?" → budget=11M ❌
"Tour quá tệ" → gợi ý tour ❌
"Tour đó được mấy ngày" (after Msg1) → re-search ❌
```

### After PHASE 1
```
"Tour Đà Lạt giá bao nhiêu?" → intent=tour_search ✅
"Tháng 11 nên đi đâu?" → destination=null, month=11, intent=recommendation ✅
"Tour quá tệ" → intent=complaint, requires_human_support=true ✅
"Tour đó được mấy ngày" → uses last_destination="Đà Lạt", references last_tour_ids ✅
```

---

## Scoring Improvement
```
BEFORE PHASE 1:
- Intent Detection: 5/10
- Context Memory: 4/10
- Complaint Handling: 1/10
- Entity Extraction: 5/10
= AVERAGE: 3.75/10

AFTER PHASE 1:
- Intent Detection: 8/10
- Context Memory: 8/10
- Complaint Handling: 9/10
- Entity Extraction: 8/10
= AVERAGE: 8.25/10
```
