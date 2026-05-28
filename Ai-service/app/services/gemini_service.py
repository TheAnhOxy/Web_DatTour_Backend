from google import genai
from google.genai import types
from app.core.config import settings
from typing import List
from app.models.chat import ChatMessage, ChatResponse, ExtractedEntities
from app.services.mock_travel_knowledge import mock_travel_assistant

def _clean_schema(schema_dict):
    """Recursively remove 'additionalProperties' from schema dict to satisfy Gemini API constraints."""
    if not isinstance(schema_dict, dict):
        return schema_dict
    
    cleaned = dict(schema_dict)
    if "additionalProperties" in cleaned:
        del cleaned["additionalProperties"]
        
    for k, v in cleaned.items():
        if isinstance(v, dict):
            cleaned[k] = _clean_schema(v)
        elif isinstance(v, list):
            cleaned[k] = [_clean_schema(item) if isinstance(item, dict) else item for item in v]
            
    return cleaned

def get_response_schema(model_class):
    if hasattr(model_class, "model_json_schema"):
        schema_dict = model_class.model_json_schema()
    else:
        schema_dict = model_class.schema()
    return _clean_schema(schema_dict)

class GeminiService:
    def __init__(self):
        if settings.GEMINI_API_KEY and settings.AI_MODE.lower() not in {"mock", "test", "demo"}:
            self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        else:
            self.client = None

    def _use_mock_backend(self) -> bool:
        return settings.AI_MODE.lower() in {"mock", "test", "demo"} or self.client is None

    async def extract_entities(self, user_message: str, chat_history: List[ChatMessage]) -> ExtractedEntities:
        if self._use_mock_backend():
            return mock_travel_assistant.extract_entities(user_message, chat_history)

        prompt = f"""
        Nhiệm vụ: Trích xuất thông tin tìm kiếm Tour từ tin nhắn người dùng.
        Tin nhắn: "{user_message}"
        
        Quy tắc trích xuất:
        - destination: Tên địa điểm du lịch (Đà Lạt, Nha Trang, Phú Quốc, Sapa, Hạ Long, Ninh Bình, Nhật Bản, v.v). KHÔNG bao gồm: tháng (tháng 11 không phải destination), số lượng người (2, 3).
        - budget: Số tiền (triệu đồng) mà user sẵn sàng chi tiêu. Ví dụ: "5 triệu", "7tr", "10 triệu". KHÔNG bao gồm: số tháng (tháng 11), số ngày đi (3 ngày), mã booking (BK123, BK456).
        - days: Số ngày đi tour (2, 3, 4, 5). KHÔNG bao gồm: tháng, mã booking, ngân sách.
        - travel_group: Nhóm đi (couple, family, friends, solo). Tìm kiếm từ khóa như: cặp đôi, người yêu, gia đình, trẻ em, nhóm bạn, một mình.
        - booking_id: Mã đặt tour (thường bắt đầu bằng BK, ví dụ: BK123, BK456).
        
        Quy tắc bổ sung:
        - Nếu không tìm thấy thông tin, set giá trị = null.
        - KHÔNG bịa ra thông tin không có trong tin nhắn.
        - Prioritize: destination > budget > days > travel_group > booking_id.
        
        Trả về JSON:
        {{"destination": <string|null>, "budget": <float|null>, "days": <int|null>, "travel_group": <string|null>, "booking_id": <string|null>}}
        """
        if not self.client:
             raise ValueError("API Key is missing.")
             
        try:
            response = self.client.models.generate_content(
                model='gemini-2.5-flash',
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=get_response_schema(ExtractedEntities),
                )
            )
            import json
            return ExtractedEntities(**json.loads(response.text))
        except Exception as e:
            print(f"Error extracting entities: {e}")
            return ExtractedEntities(destination=None, budget=None, days=None, travel_group=None, booking_id=None)

    async def chat_with_travel_assistant(self, user_message: str, chat_history: List[ChatMessage], retrieved_tours_context: str, extracted_entities: ExtractedEntities, tool_results: str = "", mode_prompt_adjustment: str = "", tone: str = "", observed_intent: str = "", observed_confidence: float = 0.0) -> ChatResponse:
        if self._use_mock_backend():
            return mock_travel_assistant.build_response(
                user_message=user_message,
                chat_history=chat_history,
                retrieved_tours_context=retrieved_tours_context,
                extracted_entities=extracted_entities,
                tool_results=tool_results,
                observed_intent=observed_intent,
                observed_confidence=observed_confidence,
            )

        from app.services.tool_registry import tool_registry
        
        # Get tool definitions
        tools_info = ""
        for func in tool_registry.get_registered_functions():
            tools_info += f"- {func.__name__}: {func.__doc__}\n"

        system_prompt = """
    You are a premium AI travel assistant.

    Response style MUST be:
    - concise
    - warm and emotionally natural
    - mobile-friendly and easy to scan

    STRICT RULES:
    - Maximum 2 short paragraphs before any bullet list
    - Maximum 3 suggested tours in `suggested_tours`
    - Maximum 1 follow-up question in `suggested_questions`
    - Never dump large text or repeat user input unnecessarily
    - Avoid robotic templates; sound like a friendly travel consultant
    - Use subtle emoji where appropriate (light and contextual)

    RESPONSE STRUCTURE (MANDATORY):
    1) Emotional acknowledgment (1 short sentence)
    2) One-line key recommendation insight (1 short sentence)
    3) Up to 3 concise tour bullets (each 1 short line: title — price — days)
    4) One lightweight CTA question (max 1)

    OUTPUT FORMAT: Return EXACTLY valid JSON matching the `ChatResponse` schema. Obey these constraints strictly: `suggested_tours` max 3, `suggested_questions` max 1, `reply` must be short (<= 3 short sentences), include `opinion` (1-2 sentence recommendation) and `confidence` (float 0.0-1.0). If you need external data (booking status, weather), add a `tool_calls` entry and LEAVE `reply` empty so the system can call the tool.

    If you do NOT know something, do NOT hallucinate — respond that data is unavailable and/or request the user to provide needed info (e.g., booking id).
    """

        prompt = f"""
    {system_prompt}

    Conversation Mode Adjustment:
    {mode_prompt_adjustment}

    Desired Tone: {tone}

    Available Tools:
    {tools_info}

    Tool results (if any):
    {tool_results}

    Context (Tours):
    ---
    {retrieved_tours_context}
    ---

    Chat History (last 3):
    {[f"User: {msg.content}" if msg.role=="user" else f"Assistant: {msg.content}" for msg in chat_history[-3:]]}

    User Message: "{user_message}"

    Return only valid JSON matching ChatResponse with the constraints above.
    """
        # Include observed intent info in prompt when available
        if observed_intent:
            prompt = prompt + f"\nObserved intent: {observed_intent} (confidence={observed_confidence})\n"

        if not self.client:
             raise ValueError("API Key is missing.")

        try:
            response = self.client.models.generate_content(
                model='gemini-2.5-flash',
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=get_response_schema(ChatResponse),
                )
            )
            
            # response.text is a JSON string matching ChatResponse schema
            # We parse it to return a valid Python object/dict
            import json
            return ChatResponse(**json.loads(response.text))
        except Exception as e:
            print(f"Error calling Gemini API: {e}")
            return ChatResponse(
                reply="Xin lỗi, hiện tại hệ thống AI đang gặp chút sự cố. Bạn vui lòng thử lại sau nhé! 😥",
                intent="other",
                sentiment="neutral",
                suggested_tours=[],
                suggested_questions=[],
                extracted_entities=ExtractedEntities(destination=None, budget=None, days=None, travel_group=None),
                tool_calls=[],
                requires_human_support=True,
                metadata=None,
                opinion=None,
                confidence=None
            )

gemini_service = GeminiService()
