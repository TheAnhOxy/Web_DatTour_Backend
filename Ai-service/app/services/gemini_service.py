from google import genai
from google.genai import types
from app.core.config import settings
from typing import List
from app.models.chat import ChatMessage, ChatResponse, ExtractedEntities
from app.services.mock_travel_knowledge import mock_travel_assistant

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
        
        Quy tắc bổ sung:
        - Nếu không tìm thấy thông tin, set giá trị = null.
        - KHÔNG bịa ra thông tin không có trong tin nhắn.
        - Prioritize: destination > budget > days > travel_group.
        
        Trả về JSON:
        {{"destination": <string|null>, "budget": <float|null>, "days": <int|null>, "travel_group": <string|null>}}
        """
        if not self.client:
             raise ValueError("API Key is missing.")
             
        try:
            response = self.client.models.generate_content(
                model='gemini-2.5-flash',
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=ExtractedEntities,
                )
            )
            import json
            return ExtractedEntities(**json.loads(response.text))
        except Exception as e:
            print(f"Error extracting entities: {e}")
            return ExtractedEntities(destination=None, budget=None, days=None, travel_group=None)

    async def chat_with_travel_assistant(self, user_message: str, chat_history: List[ChatMessage], retrieved_tours_context: str, extracted_entities: ExtractedEntities, tool_results: str = "") -> ChatResponse:
        if self._use_mock_backend():
            return mock_travel_assistant.build_response(
                user_message=user_message,
                chat_history=chat_history,
                retrieved_tours_context=retrieved_tours_context,
                extracted_entities=extracted_entities,
                tool_results=tool_results,
            )

        from app.services.tool_registry import tool_registry
        
        # Get tool definitions
        tools_info = ""
        for func in tool_registry.get_registered_functions():
            tools_info += f"- {func.__name__}: {func.__doc__}\n"

        prompt = f"""
        Bạn là "GoTour Agent", một trợ lý du lịch AI thông minh, vui vẻ, chuyên nghiệp của GoTourNow.
        
        Danh sách các công cụ (Tools) bạn có thể sử dụng:
        {tools_info}
        
        Kết quả từ công cụ (nếu có):
        {tool_results}

        Context (Các Tour trong hệ thống):
        ---
        {retrieved_tours_context}
        ---

        Lịch sử trò chuyện (sử dụng để hiểu ngữ cảnh):
        {[f"User: {{msg.content}}" if msg.role == "user" else f"Assistant: {{msg.content}}" for msg in chat_history[-3:]]}

        Câu hỏi hiện tại của khách hàng: "{user_message}"

        Yêu cầu OUTPUT (Bắt buộc):
        1. KHÔNG BAO GIỜ bịa ra (hallucinate) Tour không có trong Context.
        2. Cảm xúc (sentiment) phải là: "positive", "negative", hoặc "neutral". Nếu user dùng từ tiêu cực (tệ, chán, thất vọng, lừa), set = "negative".
        3. Intent phải là một trong: "tour_search", "tour_comparison", "booking_support", "complaint", "greeting", "recommendation", "other".
        4. Nếu sentiment = "negative" HOẶC intent = "complaint", bắt buộc set requires_human_support = true.
        5. Nếu user hỏi về thời tiết, cần tool `get_current_weather`.
        6. Nếu user muốn hủy/kiểm tra booking (có mã BK), gọi tool tương ứng.
        
        Hướng dẫn Tool calling:
        - Nếu cần gọi Tool, hãy để trống phần `reply`, đổ dữ liệu vào `tool_calls`.
           CHÚ Ý: Bắt buộc phải điền ĐẦY ĐỦ tham số vào object `arguments`.
           Ví dụ: {{"tool_name": "get_current_weather", "arguments": {{"location": "Đà Lạt"}}, "status": "pending"}}
           Ví dụ 2: {{"tool_name": "check_booking_status", "arguments": {{"booking_id": "BK123"}}, "status": "pending"}}
        - Nếu đã có `tool_results` (kết quả từ tool), sử dụng để tạo câu trả lời trong `reply`.
        
        Output JSON đầy đủ:
        {{
            "reply": "Câu trả lời tự nhiên cho user, dựa trên context + tool_results",
            "intent": "tour_search" | "tour_comparison" | "booking_support" | "complaint" | "greeting" | "recommendation" | "other",
            "sentiment": "positive" | "neutral" | "negative",
            "suggested_tours": [<danh sách 3-5 tour phù hợp>],
            "suggested_questions": [<3 câu hỏi gợi ý tiếp theo>],
            "extracted_entities": {extracted_entities.model_dump_json()},
            "tool_calls": [<danh sách tool pending, nếu có>],
            "requires_human_support": <true|false>,
            "metadata": {{"processing_time_ms": 100, "llm_used": "gemini-2.5-flash"}}
        }}
        """

        if not self.client:
             raise ValueError("API Key is missing.")

        try:
            response = self.client.models.generate_content(
                model='gemini-2.5-flash',
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=ChatResponse,
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
                metadata=None
            )

gemini_service = GeminiService()
