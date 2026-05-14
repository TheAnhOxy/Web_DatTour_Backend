from google import genai
from app.core.config import settings
from app.models.chat import ExtractedEntities
import httpx

class RagService:
    def __init__(self):
        if settings.GEMINI_API_KEY:
            self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        else:
            self.client = None
        # URL của Search-service (Java Spring Boot)
        self.search_service_url = "http://localhost:8083/search/tours"

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
                    if not tours:
                        return "Không tìm thấy tour nào phù hợp trong hệ thống."
                    
                    context_chunks = [self._build_text_for_record(t) for t in tours[:5]] # Lấy top 5
                    return "\n\n".join(context_chunks)
                else:
                    print(f"Search-service returned status code {response.status_code}")
        except Exception as e:
            print(f"Error calling Search-service: {e}")
            
        # Fallback Mock data nếu Java Service chưa chạy
        print("Using fallback mock data for RAG...")
        mock_tour = {
            "id": "TOUR_MOCK_1",
            "title": "Tour Nha Trang - Đảo Khỉ - Suối Hoa Lan (Mock)",
            "destinations": ["Nha Trang", "Đảo Khỉ"]
        }
        return self._build_text_for_record(mock_tour)

rag_service = RagService()
