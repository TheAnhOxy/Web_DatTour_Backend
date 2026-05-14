import google.generativeai as genai
from app.core.config import settings
from typing import List
from app.models.chat import ChatMessage

# Initialize Gemini
if settings.GEMINI_API_KEY:
    genai.configure(api_key=settings.GEMINI_API_KEY)

class GeminiService:
    def __init__(self):
        # We use gemini-1.5-flash (the recommended fast/cheap model)
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    async def chat_with_travel_assistant(self, user_message: str, chat_history: List[ChatMessage], retrieved_tours_context: str) -> str:
        prompt = f"""
        Bạn là "GoTour Agent", một trợ lý du lịch AI thông minh, nhiệt tình và chuyên nghiệp của nền tảng GoTourNow.
        Nhiệm vụ của bạn là tư vấn tour, giải đáp thắc mắc về địa điểm, và chốt sale cho khách hàng.

        Dưới đây là các Tour du lịch và thông tin liên quan có sẵn trong hệ thống phù hợp với câu hỏi của khách (được cung cấp từ cơ sở dữ liệu):
        ---
        {retrieved_tours_context}
        ---

        Dựa vào thông tin các Tour ở trên và lịch sử trò chuyện, hãy trả lời câu hỏi của khách hàng: "{user_message}"

        Yêu cầu:
        1. Chỉ đề xuất các Tour có trong thông tin hệ thống cung cấp ở trên. Không tự bịa ra Tour. Nếu trong danh sách không có, hãy nói là hiện chưa tìm thấy tour phù hợp và gợi ý tour khác trong hệ thống.
        2. Nếu khách hỏi thông tin ngoài luồng, hãy khéo léo dẫn dắt họ về việc đi du lịch.
        3. Giọng văn thân thiện, mời gọi, sử dụng emoji phù hợp (🏝️, ✈️, 🎒).
        4. Trình bày rõ ràng tên Tour, Giá cả, và điểm nổi bật để khách dễ đọc.
        """

        # Convert history to Gemini format (optional depending on how you use the API)
        # For a simple text generation with context, we can just pass the whole prompt.
        # If we want to use the ChatSession, we can do that too.
        # Here we just generate content directly for simplicity since the prompt contains context.
        
        try:
            response = self.model.generate_content(prompt)
            return response.text
        except Exception as e:
            print(f"Error calling Gemini API: {e}")
            return "Xin lỗi, hiện tại hệ thống AI đang gặp chút sự cố. Bạn vui lòng thử lại sau nhé! 😥"

gemini_service = GeminiService()
