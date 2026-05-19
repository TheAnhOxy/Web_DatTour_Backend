"""
Proper Intent Classifier using Gemini LLM.
MUST run AFTER negative detection, BEFORE tool routing.
Fixes: Greeting overfitting, intent router becoming tool dispatch.
"""
import json
import re
from typing import Dict, Optional
from enum import Enum
from pydantic import BaseModel
import google.generativeai as genai
from app.core.config import settings


class IntentType(str, Enum):
    """All possible intents"""
    GREETING = "greeting"
    CASUAL_CHAT = "casual_chat"
    TOUR_SEARCH = "tour_search"
    RECOMMENDATION = "recommendation"
    COMPARISON = "comparison"
    BOOKING_SUPPORT = "booking_support"
    COMPLAINT = "complaint"
    OTHER = "other"


class IntentClassificationResult(BaseModel):
    """Structured intent classification result"""
    intent: IntentType
    confidence: float  # 0.0-1.0
    reasoning: str


class IntentClassifier:
    """
    Classify user intent using Gemini LLM.
    Falls back to keyword-based detection if Gemini unavailable.
    """
    
    def __init__(self):
        """Initialize Gemini client"""
        if settings.GEMINI_API_KEY:
            genai.configure(api_key=settings.GEMINI_API_KEY)
            self.model = genai.GenerativeModel("gemini-2.5-flash")
        else:
            self.model = None
    
    async def classify(self, 
                      message: str, 
                      entities: Dict = None,
                      has_negative_sentiment: bool = False) -> IntentClassificationResult:
        """
        Classify message intent.
        
        Priority:
        1. If negative sentiment → complaint (already detected by NegativeDetector)
        2. Otherwise: Use Gemini if available, fallback to keyword
        """
        
        # ========== EARLY RETURN: COMPLAINT IF NEGATIVE ==========
        if has_negative_sentiment:
            return IntentClassificationResult(
                intent=IntentType.COMPLAINT,
                confidence=1.0,
                reasoning="Negative sentiment detected by early pipeline"
            )
        
        # ========== TRY GEMINI CLASSIFICATION ==========
        if self.model:
            try:
                return await self._classify_with_gemini(message, entities)
            except Exception as e:
                print(f"⚠️ Gemini classification failed: {e}")
                # Fall through to keyword fallback
        
        # ========== FALLBACK: KEYWORD-BASED ==========
        return self._classify_with_keywords(message)
    
    async def _classify_with_gemini(self, 
                                   message: str, 
                                   entities: Dict = None) -> IntentClassificationResult:
        """Use Gemini to classify intent"""
        
        entities_str = json.dumps(entities or {}, ensure_ascii=False)
        
        prompt = f"""Classify the following user message into ONE of these intents:

INTENT DEFINITIONS:
1. greeting: Simple greeting, introduction, or politeness. Examples: "Xin chào", "Chào bạn", "Hi", "Hello"
2. casual_chat: General conversation not directly about tours. Examples: "Bạn là ai?", "Thời tiết hôm nay sao?", "Bạn thích đi đâu?"
3. tour_search: Looking for specific tours or filtering by criteria. Examples: "Tour Đà Lạt giá bao nhiêu?", "Tìm tour 3 ngày"
4. recommendation: Asking AI to recommend based on needs. Examples: "Nên đi tour nào?", "Có gì phù hợp với gia đình?", "Tháng 11 nên đi đâu?"
5. comparison: Comparing multiple tours or asking which is better. Examples: "Tour A hay B?", "Cái nào đáng hơn?", "So sánh 2 tour này"
6. booking_support: Booking-related, payment, or status checking. Examples: "Booking BK123 ở đâu?", "Hủy đơn", "Đã thanh toán chưa?", "Tôi muốn hủy booking"
7. complaint: Already handled upstream (negative sentiment). Should not reach here unless edge case.
8. other: Doesn't fit any category above.

KEY RULES (STRICT):
- "Tour Đà Lạt giá bao nhiêu?" → tour_search (NOT greeting)
- "Bạn là ai?" with tour context → casual_chat (NOT tour_search)
- "Đi đâu đẹp?" OR "Tháng 11 nên đi đâu?" → recommendation (NOT tour_search)
- "Tour A hay B?" → comparison (NOT tour_search)
- Any question about booking status/payment → booking_support (NOT tour_search)

USER MESSAGE: "{message}"
EXTRACTED ENTITIES: {entities_str}

Respond with ONLY valid JSON (no markdown, no code blocks):
{{
    "intent": "<one of: greeting, casual_chat, tour_search, recommendation, comparison, booking_support, complaint, other>",
    "confidence": <float 0.0-1.0>,
    "reasoning": "<brief explanation>"
}}
"""
        
        response = self.model.generate_content(prompt)
        response_text = response.text.strip()
        
        # ========== PARSE JSON RESPONSE ==========
        try:
            # Try direct JSON parsing
            result_dict = json.loads(response_text)
        except json.JSONDecodeError:
            # If JSON invalid, try extracting JSON from text
            json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
            if json_match:
                result_dict = json.loads(json_match.group())
            else:
                raise ValueError(f"Could not extract JSON from: {response_text}")
        
        # ========== VALIDATE & RETURN ==========
        intent_str = result_dict.get("intent", "other").lower()
        
        # Validate intent is in allowed list
        try:
            intent = IntentType(intent_str)
        except ValueError:
            intent = IntentType.OTHER
        
        confidence = float(result_dict.get("confidence", 0.5))
        confidence = max(0.0, min(1.0, confidence))  # Clamp 0-1
        
        reasoning = str(result_dict.get("reasoning", ""))[:200]
        
        return IntentClassificationResult(
            intent=intent,
            confidence=confidence,
            reasoning=reasoning
        )
    
    def _classify_with_keywords(self, message: str) -> IntentClassificationResult:
        """
        Fallback keyword-based classification.
        IMPORTANT: This is ONLY for when Gemini fails, not primary logic!
        """
        
        msg_lower = message.lower()
        
        # ========== KEYWORD RULES (CONSERVATIVE) ==========
        
        # Booking support (high confidence keywords)
        if any(w in msg_lower for w in ["booking", "BK", "hủy", "thanh toán", 
                                         "đặt cọc", "đơn", "hoàn tiền", "refund"]):
            return IntentClassificationResult(
                intent=IntentType.BOOKING_SUPPORT,
                confidence=0.8,
                reasoning="Contains booking-related keywords"
            )
        
        # Comparison (clear signals)
        if any(w in msg_lower for w in ["so sánh", "compare", "hay hơn", "nào tốt", 
                                         "cái nào", "nên chọn cái nào", "hay", "better"]):
            return IntentClassificationResult(
                intent=IntentType.COMPARISON,
                confidence=0.8,
                reasoning="Contains comparison keywords"
            )
        
        # Greeting (very strict)
        if message.strip().lower() in ["xin chào", "chào", "hello", "hi", "hey"]:
            return IntentClassificationResult(
                intent=IntentType.GREETING,
                confidence=0.9,
                reasoning="Exact greeting match"
            )
        
        # Tour search (medium confidence)
        if any(w in msg_lower for w in ["giá", "bao nhiêu", "ngày", "nơi", "tour", 
                                          "đi", "tìm", "search"]):
            return IntentClassificationResult(
                intent=IntentType.TOUR_SEARCH,
                confidence=0.6,
                reasoning="Contains tour-related keywords"
            )
        
        # Default: casual or other
        if len(message) < 10:
            return IntentClassificationResult(
                intent=IntentType.CASUAL_CHAT,
                confidence=0.5,
                reasoning="Short message, likely casual"
            )
        
        return IntentClassificationResult(
            intent=IntentType.OTHER,
            confidence=0.3,
            reasoning="Could not classify with keywords"
        )


# ========== TESTS ==========
if __name__ == "__main__":
    import asyncio
    
    classifier = IntentClassifier()
    
    test_cases = [
        {
            "msg": "Tour Đà Lạt giá bao nhiêu?",
            "entities": {"destination": "Đà Lạt"},
            "expected": "tour_search"
        },
        {
            "msg": "Tháng 11 nên đi đâu?",
            "entities": {"month": 11},
            "expected": "recommendation"
        },
        {
            "msg": "Tour A hay B?",
            "entities": {},
            "expected": "comparison"
        },
        {
            "msg": "Booking BK123 ở đâu?",
            "entities": {"booking_id": "BK123"},
            "expected": "booking_support"
        },
        {
            "msg": "Xin chào",
            "entities": {},
            "expected": "greeting"
        },
    ]
    
    print("🧪 Intent Classifier Tests (Keyword Fallback)\n" + "="*50)
    for i, test in enumerate(test_cases, 1):
        result = classifier._classify_with_keywords(test["msg"])
        print(f"\n{i}. Input: {test['msg']}")
        print(f"   Expected: {test['expected']}")
        print(f"   Got: {result.intent.value}")
        print(f"   Confidence: {result.confidence:.2f}")
        print(f"   Reasoning: {result.reasoning}")
        status = "✅" if result.intent.value == test['expected'] else "⚠️"
        print(f"   {status}")
