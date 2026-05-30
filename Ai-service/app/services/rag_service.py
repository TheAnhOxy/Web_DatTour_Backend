from google import genai
from app.core.config import settings
from app.models.chat import ExtractedEntities
import httpx
from app.services.mock_travel_knowledge import mock_travel_assistant

class RagService:
    def __init__(self):
        if settings.GEMINI_API_KEY:
            self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        else:
            self.client = None
        # URL của Search-service (Java Spring Boot)
        self.search_service_url = settings.SEARCH_SERVICE_URL

    def _build_text_for_record(self, record_data: dict) -> str:
        # Hàm format kết quả JSON từ Java trả về thành dạng Text để nhét vào Prompt
        return (
            f"Thông tin Tour:\n"
            f"  ID: {record_data.get('id')}\n"
            f"  Tên Tour: {record_data.get('title')}\n"
            f"  Các điểm đến: {', '.join(record_data.get('destinations', []))}\n"
        )

    async def search_tours_via_adapter(self, entities: ExtractedEntities) -> str:
        """
        Gọi API sang Search Service (Java) dựa trên Entities đã trích xuất.
        Thay vì tự query Elasticsearch, AI Service giao việc này cho Backend Adapter.
        """
        params = {}
        if entities.destination:
            params['destination'] = entities.destination
            
        print(f"Calling Search-service with params: {params}")
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(self.search_service_url, params=params, timeout=5.0)
                if response.status_code == 200:
                    tours = response.json()
                    if not isinstance(tours, list):
                        print(f"Search-service returned non-list response: {tours}")
                        return "Lỗi: Dữ liệu trả về từ Search-service không đúng định dạng (không phải danh sách)."
                    if not tours:
                        return "Không tìm thấy tour nào phù hợp trong hệ thống."
                    
                    context_chunks = [self._build_text_for_record(t) for t in tours[:5]] # Lấy top 5
                    return "\n\n".join(context_chunks)
                else:
                    print(f"Search-service returned status code {response.status_code}")
                    return f"Lỗi: Dịch vụ tìm kiếm phản hồi mã lỗi {response.status_code}."
        except Exception as e:
            print(f"Error calling Search-service: {e}")
            return f"Lỗi: Không thể kết nối tới Search-service ({e})."

rag_service = RagService()
