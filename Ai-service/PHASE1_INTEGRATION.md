# 🔧 PHASE 1: Integration Guide

## Overview

Chat endpoint mới sẽ sử dụng PHASE 1 components theo thứ tự:

```
User Message
    ↓
Normalize
    ↓ (NEW)
Entity Extraction (entity_extractor.py)
    ↓ (NEW)
Negative Detection (negative_detector.py)
    ↓ (NEW)
Intent Classification (intent_classifier.py) 
    ↓ (NEW)
Conversation FSM (conversation_fsm.py)
    ↓ (NEW)
Load Session State (enhanced_memory_service.py)
    ↓
Route & Process (existing tools)
    ↓
Update Session State (enhanced_memory_service.py)
    ↓
Return Response
```

---

## Step 1: Update Imports in `chat.py`

```python
# app/api/chat.py

from app.services.entity_extractor import EntityExtractor
from app.services.negative_detector import NegativeDetector
from app.services.intent_classifier import IntentClassifier
from app.services.conversation_fsm import ConversationFSM
from app.services.enhanced_memory_service import EnhancedMemoryService
```

---

## Step 2: Initialize Components

```python
# At module level
entity_extractor = EntityExtractor()
negative_detector = NegativeDetector()
intent_classifier = IntentClassifier()
conversation_fsm = ConversationFSM()

# Use EnhancedMemoryService instead of memory_service
from app.services.enhanced_memory_service import memory_service as enhanced_memory
```

---

## Step 3: New Chat Endpoint Logic

### BEFORE (Old Flow)
```python
@app.post("/api/chat/")
async def chat(request: ChatRequest):
    # 1. Extract entities (old, buggy)
    entities = extract_entities_old(request.message)
    
    # 2. Search tours (immediate)
    tours = search_tours(entities)
    
    # 3. Generate response (template-based)
    response = build_response(tours)
    
    return response
```

### AFTER (New Flow)
```python
@app.post("/api/chat/")
async def chat(request: ChatRequest) -> ChatResponse:
    """
    PHASE 1: New chat endpoint with proper pipeline
    
    Steps:
    1. Normalize message
    2. Extract entities (with priority)
    3. Detect negative sentiment (EARLY)
    4. Classify intent (LLM)
    5. Determine conversation mode (FSM)
    6. Load session state
    7. Route & process
    8. Update session state
    9. Generate response
    """
    
    message = request.message.strip()
    session_id = request.session_id or "default"
    
    # ========== 1. NORMALIZE ==========
    message_normalized = normalize_message(message)
    
    # ========== 2. ENTITY EXTRACTION (Priority-based) ==========
    entities = entity_extractor.extract(message_normalized)
    entities_dict = entities.dict()
    
    # ========== 3. NEGATIVE DETECTION (EARLY & PRIORITY) ==========
    negative_result = negative_detector.detect(message_normalized)
    
    # ========== 4. INTENT CLASSIFICATION (LLM) ==========
    intent_result = await intent_classifier.classify(
        message_normalized,
        entities_dict,
        negative_result.has_negative_sentiment
    )
    intent = intent_result.intent.value
    
    # ========== 5. LOAD SESSION STATE ==========
    state = await enhanced_memory.load_state(session_id)
    current_mode = state.conversation_mode
    
    # ========== 6. DETERMINE CONVERSATION MODE (FSM) ==========
    next_mode = await conversation_fsm.determine_mode(intent, current_mode)
    mode_context = conversation_fsm.get_mode_context(next_mode)
    
    # ========== 7. ROUTE & PROCESS BASED ON INTENT ==========
    
    if intent == "complaint":
        # COMPLAINT HANDLER (NEW - PRIORITY)
        response = await handle_complaint(
            message=message,
            entities=entities_dict,
            negative_result=negative_result,
            state=state
        )
    
    elif intent == "booking_support":
        # BOOKING SUPPORT
        response = await handle_booking_support(
            message=message,
            entities=entities_dict,
            state=state
        )
    
    elif intent == "comparison":
        # COMPARISON HANDLER
        response = await handle_comparison(
            message=message,
            entities=entities_dict,
            last_results=state.last_tour_results,
            state=state
        )
    
    elif intent == "tour_search":
        # TOUR SEARCH
        tours = await search_tours_enhanced(entities_dict, state)
        response = await build_tour_response(
            tours=tours,
            entities=entities_dict,
            state=state
        )
    
    elif intent == "recommendation":
        # RECOMMENDATION ENGINE
        tours = await recommend_tours(entities_dict, state)
        response = await build_recommendation_response(
            tours=tours,
            entities=entities_dict,
            reasoning=intent_result.reasoning,
            state=state
        )
    
    else:  # greeting, casual_chat, other
        response = await handle_casual(
            message=message,
            state=state
        )
    
    # ========== 8. UPDATE SESSION STATE ==========
    await enhanced_memory.update_context(
        session_id=session_id,
        intent=intent,
        entities=entities_dict,
        tour_results=response.suggested_tours,
        conversation_mode=next_mode
    )
    
    # ========== 9. ADD TO CHAT HISTORY ==========
    from app.models.chat import ChatMessage
    from datetime import datetime
    
    user_msg = ChatMessage(
        role="user",
        content=request.message,
        timestamp=datetime.now()
    )
    await enhanced_memory.add_message(session_id, user_msg)
    
    assistant_msg = ChatMessage(
        role="assistant",
        content=response.reply,
        timestamp=datetime.now()
    )
    await enhanced_memory.add_message(session_id, assistant_msg)
    
    # ========== 10. RETURN RESPONSE ==========
    response.session_id = session_id
    response.intent = intent
    response.sentiment = negative_result.sentiment
    response.conversation_mode = next_mode
    
    return response
```

---

## Step 4: New Handler Functions

### 4.1 Complaint Handler (NEW)

```python
async def handle_complaint(message: str, 
                          entities: dict,
                          negative_result: dict,
                          state) -> ChatResponse:
    """
    Handle complaint with escalation.
    MUST set requires_human_support = true
    """
    
    # Create support ticket
    ticket_id = await tool_registry.create_support_ticket(
        booking_id=entities.get("booking_id") or state.last_booking_id,
        issue_type="complaint",
        description=message
    )
    
    # Build escalation response
    response = ChatResponse(
        reply=f"""Mình rất tiếc vì trải nghiệm chưa tốt của bạn.
        
Mình đã ghi nhận phản hồi và tạo ticket hỗ trợ: {ticket_id}

CSKH sẽ liên hệ bạn trong 2 giờ để hỗ trợ thêm.

Còn gì khác mình giúp được không?""",
        intent="complaint",
        sentiment=negative_result["sentiment"],
        requires_human_support=True,
        support_ticket_id=ticket_id,
        tool_calls=[{
            "tool": "create_support_ticket",
            "status": "pending",
            "ticket_id": ticket_id
        }]
    )
    
    return response
```

### 4.2 Enhanced Tour Search Handler

```python
async def search_tours_enhanced(entities: dict, state) -> list:
    """
    Search tours using entity extraction.
    Can reference last_tour_ids from session state.
    """
    
    from app.services.mock_travel_knowledge import MockTravelAssistant
    from app.services.gemini_service import GeminiService
    
    # Use mock for now (will be replaced with real search_service)
    mock_assistant = MockTravelAssistant()
    
    # Search with proper filters
    tours = await mock_assistant.search_tours(
        destination=entities.get("destination"),
        max_budget=entities.get("budget"),
        num_days=entities.get("days"),
        travel_group=entities.get("group"),
        month=entities.get("month")
    )
    
    # Rank tours (PHASE 2 feature)
    # tours = await ranking_layer.rank(tours, entities, state)
    
    return tours
```

### 4.3 Recommendation Handler

```python
async def recommend_tours(entities: dict, state) -> list:
    """
    AI-powered recommendations using Gemini.
    """
    
    from app.services.gemini_service import GeminiService
    
    gemini = GeminiService()
    
    # Get context
    user_profile = {
        "preferred_destinations": state.preferred_destinations,
        "preferred_groups": state.preferred_groups,
        "last_intent": state.last_intent,
    }
    
    # Get recommendations
    prompt = f"""
Based on user profile and filters, recommend top 3 tours.

User Profile: {json.dumps(user_profile)}
Filters: {json.dumps(entities)}

Consider: destination preference, travel group, budget, season, user profile.

Return JSON with tour IDs and reasons.
"""
    
    # ... rest of implementation
```

---

## Step 5: Update Server Startup

```python
@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    
    # Connect memory service (ENHANCED)
    await enhanced_memory.connect()
    
    # Initialize intent classifier (will test Gemini)
    intent_classifier.model  # Lazy load Gemini
    
    print("✅ PHASE 1 services initialized")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    
    await enhanced_memory.disconnect()
    print("✅ PHASE 1 services shutdown")
```

---

## Step 6: Test New Pipeline

### Using Built-in Test File

```bash
cd d:\Quan_Ly_Mon_Hoc\Kien_Truc_He_Thong\WebTour\BE\Ai-service
python test_phase1_integration.py
```

### Using Postman

Import `POSTMAN_COLLECTION.json` and run:
1. Group A (Basic) first
2. Group B (Intent) - should see improvements
3. Group F (Complaint) - should see major improvements
4. Group C (Context) - should maintain context better

---

## Migration Checklist

- [ ] Update `app/api/chat.py` with new pipeline
- [ ] Add imports for 5 new services
- [ ] Implement 5 new handler functions
- [ ] Update server startup/shutdown
- [ ] Run `test_phase1_integration.py`
- [ ] Test with Postman collection
- [ ] Verify Redis connection
- [ ] Test complaint → requires_human_support = true
- [ ] Verify entity extraction fixes (BK123, tháng 11)
- [ ] Verify intent classification improvements

---

## Expected Results After Integration

| Metric | Before | After |
|--------|--------|-------|
| Intent Detection Accuracy | 5/10 | 8/10 |
| Entity Extraction | 5/10 | 8/10 |
| Complaint Handling | 1/10 | 9/10 |
| Context Memory | 4/10 | 8/10 |
| **OVERALL** | **3/10** | **8/10** |

---

## Debugging Tips

If tests fail:

1. **Entity extraction wrong**: Check regex priority in `entity_extractor.py`
2. **Intent still wrong**: Verify Gemini prompt in `intent_classifier.py`
3. **Complaint not escalating**: Check `negative_result.has_negative_sentiment` flag
4. **Memory not saving**: Verify Redis connection in startup
5. **FSM transitions fail**: Check `TRANSITIONS` dict in `conversation_fsm.py`

---

## Files Modified/Created

### Created (NEW)
- `app/services/entity_extractor.py` ✅
- `app/services/negative_detector.py` ✅
- `app/services/intent_classifier.py` ✅
- `app/services/enhanced_memory_service.py` ✅
- `app/services/conversation_fsm.py` ✅
- `test_phase1_integration.py` ✅

### To Modify
- `app/api/chat.py` (update endpoint logic)
- `app/main.py` (update startup/shutdown)

### Unchanged (for now)
- `app/services/gemini_service.py` (still works, but routes via intent)
- `app/services/tool_registry.py` (still used)
- `app/models/chat.py` (may need minor updates)
