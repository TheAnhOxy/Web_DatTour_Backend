from google import genai
from google.genai import types
from app.core.config import settings
from typing import List
from app.models.chat import ChatMessage, ChatResponse, ExtractedEntities

class GeminiService:
    def __init__(self):
        if settings.GEMINI_API_KEY:
            self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        else:
            self.client = None

    async def extract_entities(self, user_message: str, chat_history: List[ChatMessage]) -> ExtractedEntities:
        prompt = f"""
        Nhiệm vụ của bạn là trích xuất thông tin tìm kiếm Tour từ tin nhắn của người dùng.
        Tin nhắn: "{user_message}"
        Trích xuất các thông tin: destination (địa điểm), budget (ngân sách), days (số ngày), travel_group (nhóm đi).
        Trả về đúng định dạng JSON.
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
        from app.services.tool_registry import tool_registry
        
        # Get tool definitions
        tools_info = ""
        for func in tool_registry.get_registered_functions():
            tools_info += f"- {func.__name__}: {func.__doc__}\n"

        prompt = f"""
        Bạn là "GoTour Agent", một trợ lý du lịch AI thông minh của GoTourNow.
        
        Danh sách các công cụ (Tools) bạn có thể sử dụng:
        {tools_info}
        
        Kết quả từ công cụ (nếu có):
        {tool_results}

        Context (Các Tour trong hệ thống):
        ---
        {retrieved_tours_context}
        ---

        Lịch sử trò chuyện:
        (Xem trong session)

        Câu hỏi hiện tại của khách hàng: "{user_message}"

        Yêu cầu BẮT BUỘC:
        1. KHÔNG BAO GIỜ bịa ra (hallucinate) Tour không có trong Context.
        2. Nếu bạn cần gọi Tool, hãy để trống phần `reply`, và điền thông tin vào `tool_calls`.
           CHÚ Ý: Bắt buộc phải điền ĐẦY ĐỦ tham số vào object `arguments`.
           Ví dụ: {{"tool_name": "get_current_weather", "arguments": {{"location": "Đà Lạt"}}, "status": "pending"}}
           Ví dụ 2: {{"tool_name": "check_booking_status", "arguments": {{"booking_id": "BK123"}}, "status": "pending"}}
        3. Nếu đã có `tool_results` (kết quả trả về từ tool), hãy dùng nó để trả lời khách hàng vào trường `reply`.
        4. Ghi nhận lại các thực thể đã trích xuất sau vào trường `extracted_entities`: {extracted_entities.model_dump_json()}
        5. Đánh giá cảm xúc (sentiment) của người dùng: positive, neutral, negative.
        6. Xác định ý định (intent): tour_search, tour_comparison, booking_support, complaint, greeting, other.
        7. Nếu user than phiền (sentiment: negative hoặc intent: complaint), set `requires_human_support` = true.
        8. Trả về đúng cấu trúc JSON đã được định nghĩa.
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
