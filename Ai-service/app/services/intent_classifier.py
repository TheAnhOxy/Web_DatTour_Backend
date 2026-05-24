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
from google import genai
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
    matched_rules: Optional[list] = []
    why: Optional[str] = None


class IntentClassifier:
    """
    Classify user intent using Gemini LLM.
    Falls back to keyword-based detection if Gemini unavailable.
    """
    
    def __init__(self):
        """Initialize Gemini client"""
        if settings.GEMINI_API_KEY:
            self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        else:
            self.client = None
    
    async def classify(self, 
                      message: str, 
                      entities: Dict = None,
                      has_negative_sentiment: bool = False) -> IntentClassificationResult:
        """
        Classify message intent.
        
        Priority:
        complaint -> booking_support -> comparison -> recommendation -> tour_search -> greeting -> other
        """

        normalized = self._normalize_text(message)
        negative_score = self.detect_negative_score(message)
        if has_negative_sentiment or negative_score >= 0.75:
            return IntentClassificationResult(
                intent=IntentType.COMPLAINT,
                confidence=1.0 if negative_score >= 0.75 else 0.9,
                reasoning="Complaint shortcut triggered by negative patterns",
                matched_rules=["negative_shortcut"],
                why="matched complaint regex or high negative score"
            )

        priority_result = self._classify_by_priority(normalized)
        if priority_result is not None:
            return priority_result
        
        # ========== TRY GEMINI CLASSIFICATION ==========
        if self.client:
            try:
                return await self._classify_with_gemini(message, entities)
            except Exception as e:
                print(f"⚠️ Gemini classification failed: {e}")
                # Fall through to keyword fallback
        
        # ========== FALLBACK: KEYWORD-BASED ==========
        return self._classify_with_keywords(message)

    def _normalize_text(self, message: str) -> str:
        text = message.lower().strip()
        return re.sub(r"\s+", " ", text)

    def _classify_by_priority(self, normalized_text: str) -> Optional[IntentClassificationResult]:
        if normalized_text in {"hi", "hello", "xin chào", "chào", "alo"}:
            return IntentClassificationResult(
                intent=IntentType.GREETING,
                confidence=0.98,
                reasoning="Exact greeting match",
                matched_rules=["greeting_exact"],
                why="exact short greeting only"
            )

        if re.search(r"\b(bk\d+|booking|đặt cọc|thanh toán|hủy|huy|refund|hoàn tiền)\b", normalized_text):
            return IntentClassificationResult(
                intent=IntentType.BOOKING_SUPPORT,
                confidence=0.9,
                reasoning="Booking/support phrase matched",
                matched_rules=["booking_priority"],
                why="matched booking/support keyword"
            )

        if re.search(r"(so sánh|compare|hay hơn|nào tốt|cái nào|nên chọn cái nào|which .* better|better)", normalized_text):
            return IntentClassificationResult(
                intent=IntentType.COMPARISON,
                confidence=0.88,
                reasoning="Comparison phrase matched",
                matched_rules=["comparison_priority"],
                why="matched comparison phrase"
            )

        recommendation_patterns = [
            r"đi đâu đẹp",
            r"nên đi đâu",
            r"tour nào hot",
            r"muốn chill",
            r"thích thiên nhiên",
            r"đi biển",
            r"du lịch nghỉ dưỡng",
            r"nên đi",
            r"tháng\s*\d+\s*nên đi đâu",
            r"muốn đi",
            r"gợi ý.*tour",
            r"recommend",
            r"suggest",
        ]
        if any(re.search(p, normalized_text) for p in recommendation_patterns):
            return IntentClassificationResult(
                intent=IntentType.RECOMMENDATION,
                confidence=0.86,
                reasoning="Matched explicit recommendation phrase",
                matched_rules=["recommendation_phrase"],
                why="matched explicit recommendation phrase"
            )

        if re.search(r"(budget|ngân sách|triệu|tr\b|mùa|tháng\s*\d+|gia đình|người yêu|cặp đôi|honeymoon)", normalized_text):
            if re.search(r"(đi|tour|gợi ý|nên|phù hợp|thích|muốn)", normalized_text):
                return IntentClassificationResult(
                    intent=IntentType.RECOMMENDATION,
                    confidence=0.78,
                    reasoning="Preference/budget/season bias to recommendation",
                    matched_rules=["recommendation_bias"],
                    why="matched preference/budget/season context"
                )

        if any(w in normalized_text for w in ["giá", "bao nhiêu", "ngày", "nơi", "tour", "đi", "tìm", "search"]):
            return IntentClassificationResult(
                intent=IntentType.TOUR_SEARCH,
                confidence=0.62,
                reasoning="Contains tour-related keywords",
                matched_rules=["tour_keyword"],
                why="generic tour keywords without strong recommendation signals"
            )

        return None
    
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
        
        response = self.client.models.generate_content(
            model='gemini-2.5-flash',
            contents=prompt,
        )
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
            reasoning=reasoning,
            why=reasoning
        )
    
    def _classify_with_keywords(self, message: str) -> IntentClassificationResult:
        """
        Fallback keyword-based classification.
        IMPORTANT: This is ONLY for when Gemini fails, not primary logic!
        """
        
        msg_lower = self._normalize_text(message)
        tokens = re.findall(r"\w+", message)
        
        priority_result = self._classify_by_priority(msg_lower)
        if priority_result is not None:
            return priority_result
        
        # Default: casual or other
        if len(message) < 10:
            return IntentClassificationResult(
                intent=IntentType.CASUAL_CHAT,
                confidence=0.5,
                reasoning="Short message, likely casual",
                why="short message and no priority pattern"
            )
        
        return IntentClassificationResult(
            intent=IntentType.OTHER,
            confidence=0.3,
            reasoning="Could not classify with keywords",
            matched_rules=[],
            why="no deterministic keyword rule matched"
        )

    def detect_negative_score(self, message: str) -> float:
        """Return a negative sentiment score 0.0-1.0 based on regex and soft cues."""
        text = self._normalize_text(message)
        score = 0.0
        matched = []

        negative_patterns = [
            r"phản hồi.*chậm",
            r"khách sạn.*bẩn",
            r"phòng.*bẩn",
            r"tour.*tệ",
            r"quá tệ",
            r"rất tệ",
            r"thất vọng",
            r"mất đồ",
            r"thiếu chuyên nghiệp",
            r"không hài lòng",
            r"không như mong đợi",
            r"không ổn",
            r"cần gặp quản lý",
            r"muốn khiếu nại",
            r"hoàn tiền",
            r"refund",
            r"dịch vụ quá chậm",
            r"tour không giống mô tả",
        ]
        soft_negative_words = [
            "hơi tệ",
            "không ổn",
            "dơ",
            "bẩn",
            "chậm",
            "khó chịu",
            "chưa tốt",
            "không vui lắm",
            "hơi dơ",
            "phản hồi chậm",
            "trải nghiệm chưa tốt",
        ]

        for pattern in negative_patterns:
            if re.search(pattern, text):
                score += 0.4
                matched.append(pattern)

        for word in soft_negative_words:
            if word in text:
                score += 0.2
                matched.append(word)

        if re.search(r"(khiếu nại|gặp quản lý|cskh|hoàn tiền|refund)", text):
            score += 0.25
            matched.append("escalation_request")

        if len(matched) >= 2:
            score += 0.15

        return min(1.0, score)


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
