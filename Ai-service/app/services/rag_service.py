# This service will handle vector embeddings and MongoDB Atlas Vector Search
import google.generativeai as genai
from app.core.config import settings

class RagService:
    def __init__(self):
        # Configure genai for embeddings if needed
        pass

    def _build_text_for_record(self, record_type: str, record_data: dict) -> str:
        if record_type == "tour_package":
            return (
                f"Thông tin Tour Du Lịch:\n"
                f"  Tên Tour: {record_data.get('name')}\n"
                f"  Địa điểm đến: {record_data.get('destination')}\n"
                f"  Giá: {record_data.get('price')} VNĐ\n"
                f"  Thời gian: {record_data.get('duration_days')} ngày {record_data.get('duration_nights')} đêm\n"
                f"  Lịch trình tóm tắt: {record_data.get('itinerary_summary')}\n"
                f"  Đánh giá: {record_data.get('rating')}/5 sao\n"
                f"  Phong cách: {record_data.get('travel_style')} (Ví dụ: Nghỉ dưỡng, Khám phá...)"
            )
        elif record_type == "location_info":
            return (
                f"Thông tin Địa điểm:\n"
                f"  Tên địa điểm: {record_data.get('location_name')}\n"
                f"  Mô tả: {record_data.get('description')}\n"
                f"  Khí hậu/Thời tiết hiện tại: {record_data.get('weather')}"
            )
        return ""

    async def get_embedding(self, text: str) -> list[float]:
        """
        Calls Google's embedding model to get vector for text
        """
        try:
            result = genai.embed_content(
                model="models/text-embedding-004",
                content=text,
                task_type="retrieval_query",
            )
            return result['embedding']
        except Exception as e:
            print(f"Error getting embedding: {e}")
            return []

    async def search_tours_in_mongo(self, query_text: str) -> str:
        """
        1. Get embedding for query_text
        2. Perform $vectorSearch in MongoDB Atlas
        3. Format the result into a context string
        """
        # MOCK IMPLEMENTATION FOR NOW until MongoDB Atlas is fully connected
        print(f"Searching tours for query: {query_text}")
        
        # In a real scenario, you would do:
        # query_vector = await self.get_embedding(query_text)
        # db = get_database()
        # results = await db.tours.aggregate([ { "$vectorSearch": { ... } } ]).to_list(length=3)
        # return "\n\n".join([self._build_text_for_record("tour_package", r) for r in results])
        
        # Mock Context
        mock_tour = {
            "name": "Tour Nha Trang - Đảo Khỉ - Suối Hoa Lan",
            "destination": "Nha Trang",
            "price": "3500000",
            "duration_days": 3,
            "duration_nights": 2,
            "itinerary_summary": "Ngày 1: Tham quan tháp bà Ponagar. Ngày 2: Lặn ngắm san hô đảo Khỉ. Ngày 3: Tắm bùn khoáng.",
            "rating": 4.8,
            "travel_style": "Biển đảo, Nghỉ dưỡng"
        }
        return self._build_text_for_record("tour_package", mock_tour)

rag_service = RagService()
