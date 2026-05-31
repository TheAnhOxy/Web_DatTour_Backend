import json
import re
import unicodedata
from typing import Any, Dict, List, Optional

from app.models.chat import (
    ChatMessage,
    ChatMetadata,
    ChatResponse,
    ExtractedEntities,
    SuggestedQuestion,
    ToolCall,
    TourSummary,
)


MOCK_TOURS: List[Dict[str, Any]] = [
    {
        "id": "TOUR-DL-01",
        "title": "Tour Đà Lạt 3N2D - Lãng mạn cho cặp đôi",
        "location": "Đà Lạt",
        "price": 3900000,
        "duration_days": 3,
        "rating": 4.8,
        "tags": ["đà lạt", "cặp đôi", "thiên nhiên", "3n2d", "nghỉ dưỡng"],
        "thumbnail_url": None,
        "hotels": "Khách sạn 4 sao",
        "included_flight": False,
        "season_note": "Mát mẻ quanh năm, đẹp nhất từ tháng 10 đến tháng 3.",
        "group": "couple",
        "highlights": ["Hồ Tuyền Lâm", "Mongo Land", "vườn hoa"],
    },
    {
        "id": "TOUR-NH-01",
        "title": "Tour Nha Trang 4N3D - Biển xanh cho gia đình",
        "location": "Nha Trang",
        "price": 5800000,
        "duration_days": 4,
        "rating": 4.7,
        "tags": ["nha trang", "biển", "gia đình", "trẻ em", "4n3d"],
        "thumbnail_url": None,
        "hotels": "Resort 4 sao",
        "included_flight": True,
        "season_note": "Phù hợp đi biển vào mùa khô.",
        "group": "family",
        "highlights": ["VinWonders", "Đảo Hòn Mun", "tắm biển"],
    },
    {
        "id": "TOUR-PQ-01",
        "title": "Tour Phú Quốc 4N3D - Nghỉ dưỡng biển",
        "location": "Phú Quốc",
        "price": 8900000,
        "duration_days": 4,
        "rating": 4.9,
        "tags": ["phú quốc", "biển", "resort", "nghỉ dưỡng", "cặp đôi"],
        "thumbnail_url": None,
        "hotels": "Resort 5 sao",
        "included_flight": True,
        "season_note": "Lý tưởng cho nghỉ dưỡng, biển đẹp và lịch trình nhẹ.",
        "group": "couple",
        "highlights": ["Grand World", "Hòn Thơm", "cáp treo"],
    },
    {
        "id": "TOUR-SAPA-01",
        "title": "Tour Sapa 3N2D - Núi rừng cho nhóm bạn",
        "location": "Sapa",
        "price": 5100000,
        "duration_days": 3,
        "rating": 4.6,
        "tags": ["sapa", "núi", "trekking", "nhóm bạn", "3n2d"],
        "thumbnail_url": None,
        "hotels": "Khách sạn 3 sao",
        "included_flight": False,
        "season_note": "Mùa lúa đẹp từ tháng 9 đến tháng 10.",
        "group": "friends",
        "highlights": ["Fansipan", "bản Cát Cát", "ruộng bậc thang"],
    },
    {
        "id": "TOUR-HL-01",
        "title": "Tour Hạ Long 2N1D - Tiết kiệm cuối tuần",
        "location": "Hạ Long",
        "price": 2900000,
        "duration_days": 2,
        "rating": 4.5,
        "tags": ["hạ long", "tiết kiệm", "cuối tuần", "ngắn ngày", "biển"],
        "thumbnail_url": None,
        "hotels": "Khách sạn 3 sao",
        "included_flight": False,
        "season_note": "Phù hợp đi ngắn ngày, chi phí thấp.",
        "group": "budget",
        "highlights": ["vịnh Hạ Long", "du thuyền", "hang động"],
    },
    {
        "id": "TOUR-NB-01",
        "title": "Tour Ninh Bình 2N1D - Thiên nhiên nhẹ nhàng",
        "location": "Ninh Bình",
        "price": 2500000,
        "duration_days": 2,
        "rating": 4.7,
        "tags": ["ninh bình", "thiên nhiên", "gia đình", "ngắn ngày"],
        "thumbnail_url": None,
        "hotels": "Khách sạn 3 sao",
        "included_flight": False,
        "season_note": "Lịch trình nhẹ, hợp gia đình và người lớn tuổi.",
        "group": "family",
        "highlights": ["Tràng An", "Tam Cốc", "chùa Bái Đính"],
    },
    {
        "id": "TOUR-JP-01",
        "title": "Tour Nhật Bản 5N4D - Tokyo & Osaka",
        "location": "Nhật Bản",
        "price": 18900000,
        "duration_days": 5,
        "rating": 4.9,
        "tags": ["nhật bản", "tokyo", "osaka", "cao cấp", "khám phá"],
        "thumbnail_url": None,
        "hotels": "Khách sạn 4 sao",
        "included_flight": True,
        "season_note": "Phù hợp khách muốn đi quốc tế và cần visa.",
        "group": "family",
        "highlights": ["Shibuya", "Osaka Castle", "shopping"],
    },
]

MOCK_BOOKINGS: Dict[str, Dict[str, Any]] = {
    "BK123": {"booking_id": "BK123", "status": "confirmed", "tour_id": "TOUR-DL-01"},
    "BK456": {"booking_id": "BK456", "status": "pending_payment", "tour_id": "TOUR-NH-01"},
    "BK789": {"booking_id": "BK789", "status": "cancelled", "tour_id": "TOUR-PQ-01"},
}

GREETINGS = ["xin chào", "chao", "hello", "hi", "chào bạn", "ey"]
NEGATIVE_WORDS = ["tệ", "chán", "thất vọng", "quá tệ", "không hài lòng", "bực", "tồi", "lừa", "scam", "muộn"]
COMPARISON_WORDS = ["so sánh", "khác gì", "hơn", "đáng tiền", "nên chọn", "tour nào hơn"]
RECOMMEND_WORDS = ["gợi ý", "recommend", "nên đi đâu", "đi đâu", "tháng", "mùa", "hot"]
BOOKING_WORDS = ["booking", "đơn", "hủy", "huy", "đổi lịch", "thanh toán", "đã thanh toán", "trạng thái"]
WEATHER_WORDS = ["thời tiết", "weather", "nhiệt độ"]


def _normalize(text: str) -> str:
    text = unicodedata.normalize("NFD", text.lower())
    text = "".join(ch for ch in text if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", text).strip()


def _format_money(vnd: float) -> str:
    return f"{vnd:,.0f}".replace(",", ".") + "đ"


class MockTravelAssistant:
    def __init__(self) -> None:
        self.tours = MOCK_TOURS
        self.booking_records = MOCK_BOOKINGS

    def _last_user_messages(self, chat_history: List[ChatMessage]) -> List[str]:
        return [msg.content for msg in chat_history if msg.role == "user" and msg.content]

    def _extract_destination(self, message: str, chat_history: List[ChatMessage]) -> Optional[str]:
        normalized_message = _normalize(message)
        candidates = [tour["location"] for tour in self.tours]
        for candidate in candidates:
            if _normalize(candidate) in normalized_message:
                return candidate

        for previous in reversed(self._last_user_messages(chat_history)):
            normalized_previous = _normalize(previous)
            for candidate in candidates:
                if _normalize(candidate) in normalized_previous:
                    return candidate
        return None

    def _extract_budget_from_history(self, chat_history: List[ChatMessage]) -> Optional[float]:
        for previous in reversed(self._last_user_messages(chat_history)):
            budget = self._extract_budget(previous)
            if budget:
                return budget
        return None

    def _extract_days_from_history(self, chat_history: List[ChatMessage]) -> Optional[int]:
        for previous in reversed(self._last_user_messages(chat_history)):
            days = self._extract_days(previous)
            if days:
                return days
        return None

    def _extract_travel_group_from_history(self, chat_history: List[ChatMessage]) -> Optional[str]:
        for previous in reversed(self._last_user_messages(chat_history)):
            travel_group = self._extract_travel_group(previous)
            if travel_group:
                return travel_group
        return None

    def _extract_budget(self, message: str) -> Optional[float]:
        normalized = _normalize(message)
        match = re.search(r"(\d+(?:[.,]\d+)?)\s*(tr|trieu|trieu dong|m|trd)?", normalized)
        if not match:
            return None
        amount = float(match.group(1).replace(",", "."))
        unit = match.group(2) or ""
        if unit in {"tr", "trieu", "trieu dong", "m", "trd"} or amount < 1000:
            return amount * 1000000
        return amount

    def _extract_days(self, message: str) -> Optional[int]:
        normalized = _normalize(message)
        match = re.search(r"(\d+)\s*(n\d+d|ngay|dem|d)", normalized)
        if match:
            return int(match.group(1))
        match = re.search(r"(\d+)\s*day", normalized)
        if match:
            return int(match.group(1))
        return None

    def _extract_travel_group(self, message: str) -> Optional[str]:
        normalized = _normalize(message)
        group_map = {
            "couple": ["cap doi", "nguoi yeu", "cặp đôi", "honeymoon"],
            "family": ["gia dinh", "gia đình", "tre em", "trẻ em", "vo chong va con"],
            "friends": ["nhom ban", "nhóm bạn", "ban be", "bạn bè", "team"],
            "solo": ["mot minh", "một mình", "solo"],
        }
        for group, keywords in group_map.items():
            if any(keyword in normalized for keyword in keywords):
                return group
        return None

    def extract_entities(self, user_message: str, chat_history: List[ChatMessage]) -> ExtractedEntities:
        destination = self._extract_destination(user_message, chat_history)
        budget = self._extract_budget(user_message) or self._extract_budget_from_history(chat_history)
        days = self._extract_days(user_message) or self._extract_days_from_history(chat_history)
        travel_group = self._extract_travel_group(user_message) or self._extract_travel_group_from_history(chat_history)

        if not destination:
            for previous in reversed(self._last_user_messages(chat_history)):
                destination = self._extract_destination(previous, [])
                if destination:
                    break

        return ExtractedEntities(
            destination=destination,
            budget=budget,
            days=days,
            travel_group=travel_group,
        )

    def detect_sentiment(self, user_message: str) -> str:
        normalized = _normalize(user_message)
        if any(word in normalized for word in NEGATIVE_WORDS):
            return "negative"
        if any(word in normalized for word in ["cảm ơn", "cam on", "tuyệt", "ok", "được", "tot"]):
            return "positive"
        return "neutral"

    def detect_intent(self, user_message: str, entities: ExtractedEntities) -> str:
        normalized = _normalize(user_message)
        if any(word in normalized for word in GREETINGS):
            return "greeting"
        if any(word in normalized for word in NEGATIVE_WORDS):
            return "complaint"
        if any(word in normalized for word in COMPARISON_WORDS):
            return "tour_comparison"
        if any(word in normalized for word in BOOKING_WORDS):
            return "booking_support"
        if any(word in normalized for word in RECOMMEND_WORDS) or any(
            phrase in normalized
            for phrase in [
                "di dau dep",
                "nen di dau",
                "di dau cho dep",
                "thang nao dep",
                "mua nao dep",
                "di luc nao",
            ]
        ):
            return "recommendation"
        if entities.destination or entities.budget or entities.days or entities.travel_group:
            return "tour_search"
        if any(word in normalized for word in ["tour", "lịch trình", "lich trinh", "đi", "di", "gợi ý", "goi y"]):
            return "tour_search"
        if any(phrase in normalized for phrase in ["ban la ai", "ban co the lam gi", "tro chuyen", "chat vui"]):
            return "casual_chat"
        return "other"

    def _tour_matches(self, tour: Dict[str, Any], entities: ExtractedEntities, message: str) -> int:
        score = 0
        normalized = _normalize(message)
        if entities.destination:
            destination_norm = _normalize(entities.destination)
            if destination_norm in _normalize(tour["location"]):
                score += 6
            if any(destination_norm in _normalize(tag) for tag in tour["tags"]):
                score += 4
        if entities.budget:
            if tour["price"] <= entities.budget:
                score += 4
            elif tour["price"] <= entities.budget * 1.2:
                score += 2
        if entities.days:
            if tour["duration_days"] == entities.days:
                score += 4
            elif abs(tour["duration_days"] - entities.days) == 1:
                score += 2
        if entities.travel_group and entities.travel_group == tour.get("group"):
            score += 3
        for tag in tour["tags"]:
            if _normalize(tag) in normalized:
                score += 1
        return score

    def search_tours(self, entities: ExtractedEntities, user_message: str) -> List[Dict[str, Any]]:
        ranked = [
            (self._tour_matches(tour, entities, user_message), tour)
            for tour in self.tours
        ]
        ranked.sort(key=lambda item: (-item[0], -item[1]["rating"], item[1]["price"]))
        matches = [tour for score, tour in ranked if score > 0]
        if matches:
            return matches[:5]
        return [tour for _, tour in ranked[:3]]

    def build_tour_context(self, entities: ExtractedEntities, user_message: str) -> str:
        tours = self.search_tours(entities, user_message)
        chunks = []
        for tour in tours:
            chunks.append(
                "\n".join([
                    "Thông tin Tour:",
                    f"  ID: {tour['id']}",
                    f"  Tên Tour: {tour['title']}",
                    f"  Địa điểm: {tour['location']}",
                    f"  Giá: {_format_money(tour['price'])}",
                    f"  Số ngày: {tour['duration_days']}",
                    f"  Khách sạn: {tour['hotels']}",
                    f"  Điểm nổi bật: {', '.join(tour['highlights'])}",
                ])
            )
        return "\n\n".join(chunks)

    def _tour_to_summary(self, tour: Dict[str, Any]) -> TourSummary:
        return TourSummary(
            id=tour["id"],
            title=tour["title"],
            location=tour["location"],
            price=tour["price"],
            duration_days=tour["duration_days"],
            rating=tour["rating"],
            tags=tour["tags"],
            thumbnail_url=tour["thumbnail_url"],
        )

    def _suggestions_for(self, intent: str, entities: Optional[ExtractedEntities] = None) -> List[SuggestedQuestion]:
        if intent == "tour_search":
            if entities and entities.destination:
                items = [
                    f"Tìm tour đi {entities.destination} giá rẻ nhất",
                    f"Gợi ý tour {entities.destination} 3 ngày 2 đêm",
                    f"Tour {entities.destination} có gì nổi bật?",
                ]
            else:
                items = [
                    "Tìm tour du lịch biển mùa này",
                    "Tìm tour du lịch miền núi khám phá",
                    "Tìm các tour du lịch ngắn ngày",
                ]
        elif intent == "tour_comparison":
            items = [
                "So sánh các tour theo giá tiền",
                "Nên chọn tour nghỉ dưỡng nào tốt hơn?",
            ]
        elif intent == "booking_support":
            items = [
                "Kiểm tra trạng thái đặt tour của tôi",
                "Hướng dẫn hủy/đổi lịch đặt tour",
                "Xem lịch sử thanh toán đơn hàng",
            ]
        elif intent == "complaint":
            items = [
                "Tôi muốn khiếu nại chất lượng dịch vụ",
                "Kết nối tôi với nhân viên hỗ trợ trực tiếp",
            ]
        elif intent == "recommendation":
            items = [
                "Gợi ý các tour hot nhất hiện nay",
                "Có tour nào phù hợp đi với gia đình không?",
                "Tư vấn địa điểm du lịch tháng này",
            ]
        elif intent == "greeting":
            items = [
                "Tư vấn tour du lịch phù hợp",
                "Tìm tour giá tốt cuối tuần",
                "Gợi ý các điểm du lịch khám phá",
            ]
        else:
            items = [
                "Gợi ý các tour đi nước ngoài",
                "Tìm tour du lịch trọn gói",
            ]
        return [SuggestedQuestion(question=item) for item in items[:3]]


    def _build_reply_from_tours(self, intent: str, entities: ExtractedEntities, user_message: str) -> tuple[str, List[Dict[str, Any]]]:
        tours = self.search_tours(entities, user_message)
        if not tours:
            return (
                "Mình chưa tìm thấy tour phù hợp ngay lúc này. Bạn cho mình thêm điểm đến, ngân sách hoặc số ngày nhé.",
                [],
            )

        top_lines = []
        for index, tour in enumerate(tours[:3], start=1):
            reason_parts = []
            if entities.destination and _normalize(entities.destination) in _normalize(tour["location"]):
                reason_parts.append(f"đúng điểm đến {tour['location']}")
            if entities.budget and tour["price"] <= entities.budget:
                reason_parts.append(f"nằm trong ngân sách {_format_money(entities.budget)}")
            if entities.days and tour["duration_days"] == entities.days:
                reason_parts.append(f"đúng {entities.days} ngày")
            if entities.travel_group and entities.travel_group == tour["group"]:
                reason_parts.append(f"phù hợp nhóm {entities.travel_group}")
            reason_text = "; ".join(reason_parts) if reason_parts else f"điểm rating {tour['rating']}"
            top_lines.append(
                f"{index}. {tour['title']} - {_format_money(tour['price'])} - {tour['duration_days']}N - {reason_text}."
            )

        lead_line = "Mình chọn ra 3 phương án sát nhu cầu nhất cho bạn:"
        if intent == "recommendation":
            lead_line = f"Mình nghiêng về {tours[0]['location']} cho bạn vì {tours[0]['season_note'].rstrip('.')}"

        if intent == "tour_comparison" and len(tours) >= 2:
            first, second = tours[0], tours[1]
            reply = (
                f"Mình so sánh nhanh 2 lựa chọn nổi bật cho bạn:\n"
                f"- {first['title']}: {_format_money(first['price'])}, {first['duration_days']} ngày, {first['hotels']}.\n"
                f"- {second['title']}: {_format_money(second['price'])}, {second['duration_days']} ngày, {second['hotels']}.\n"
                f"Nếu ưu tiên tiết kiệm thì chọn {first['title'] if first['price'] <= second['price'] else second['title']}. "
                f"Nếu ưu tiên trải nghiệm/tiện nghi thì tour còn lại hợp hơn."
            )
            return reply, tours[:2]

        if intent == "recommendation":
            lead = tours[0]
            reply = (
                f"{lead_line}.\n"
                f"Giá {_format_money(lead['price'])}, phù hợp để bạn cân bằng giữa ngân sách và trải nghiệm.\n"
                + "\n".join(top_lines)
            )
            return reply, tours

        reply = (
            f"{lead_line}\n"
            + "\n".join(top_lines)
            + "\nBạn muốn mình lọc tiếp theo giá, số ngày hay loại hình tour không?"
        )
        return reply, tours

    def _booking_id_from_message(self, user_message: str) -> Optional[str]:
        normalized = user_message.upper().replace(" ", "")
        match = re.search(r"(BK\d{3,})", normalized)
        if match:
            return match.group(1)
        return None

    def _parse_tool_results(self, tool_results: str) -> Dict[str, Any]:
        if not tool_results:
            return {}
        try:
            parsed = json.loads(tool_results)
            return parsed if isinstance(parsed, dict) else {"result": parsed}
        except Exception:
            return {"raw": tool_results}

    def build_response(
        self,
        user_message: str,
        chat_history: List[ChatMessage],
        retrieved_tours_context: str,
        extracted_entities: ExtractedEntities,
        tool_results: str = "",
        observed_intent: str = "",
        observed_confidence: float = 0.0,
    ) -> ChatResponse:
        entities = self.extract_entities(user_message, chat_history)
        if extracted_entities.destination and not entities.destination:
            entities.destination = extracted_entities.destination
        if extracted_entities.budget and not entities.budget:
            entities.budget = extracted_entities.budget
        if extracted_entities.days and not entities.days:
            entities.days = extracted_entities.days
        if extracted_entities.travel_group and not entities.travel_group:
            entities.travel_group = extracted_entities.travel_group

        intent = self.detect_intent(user_message, entities)
        # If an external classifier provided a high-confidence observed_intent, trust it
        try:
            if observed_intent and observed_confidence and observed_confidence >= 0.7:
                intent = observed_intent
        except Exception:
            pass
        sentiment = self.detect_sentiment(user_message)
        parsed_tool_results = self._parse_tool_results(tool_results)
        tool_calls: List[ToolCall] = []
        suggested_tours: List[TourSummary] = []
        requires_human_support = sentiment == "negative" or intent == "complaint"
        reply = ""

        if parsed_tool_results:
            if "check_booking_status" in parsed_tool_results:
                reply = f"Mình đã kiểm tra booking cho bạn: {parsed_tool_results['check_booking_status']}"
            elif "cancel_booking" in parsed_tool_results:
                reply = f"Mình đã xử lý hủy tour: {parsed_tool_results['cancel_booking']}"
            elif "create_support_ticket" in parsed_tool_results:
                reply = f"Mình đã ghi nhận hỗ trợ: {parsed_tool_results['create_support_ticket']}"
            elif "get_current_weather" in parsed_tool_results:
                reply = f"Thông tin thời tiết hiện tại: {parsed_tool_results['get_current_weather']}"
            elif "result" in parsed_tool_results:
                reply = f"Mình đã nhận kết quả xử lý: {parsed_tool_results['result']}"
            elif "raw" in parsed_tool_results:
                reply = f"Mình đã nhận kết quả xử lý: {parsed_tool_results['raw']}"

        if not reply:
            normalized = _normalize(user_message)
            if any(word in normalized for word in WEATHER_WORDS):
                location = entities.destination or self._extract_destination(user_message, chat_history) or "điểm đến bạn quan tâm"
                tool_calls = [ToolCall(tool_name="get_current_weather", arguments={"location": location}, status="pending")]
                reply = ""
            elif intent == "booking_support":
                booking_id = self._booking_id_from_message(user_message)
                if booking_id and any(word in normalized for word in ["huy", "hủy", "cancel"]):
                    tool_calls = [
                        ToolCall(
                            tool_name="cancel_booking",
                            arguments={"bookingCode": booking_id, "booking_id": booking_id, "reason": user_message},
                            status="pending",
                        )
                    ]
                elif booking_id:
                    tool_calls = [
                        ToolCall(
                            tool_name="check_booking_status",
                            arguments={"bookingCode": booking_id, "booking_id": booking_id},
                            status="pending",
                        )
                    ]
                else:
                    reply = "Bạn cho mình mã booking để mình kiểm tra hoặc hỗ trợ hủy/đổi lịch nhé."
            elif intent == "complaint":
                booking_id = self._booking_id_from_message(user_message) or ""
                tool_calls = [
                    ToolCall(
                        tool_name="create_support_ticket",
                        arguments={"booking_id": booking_id, "issue_type": "complaint", "description": user_message},
                        status="pending",
                    )
                ]
                reply = "Mình rất tiếc vì trải nghiệm chưa tốt. Mình sẽ ghi nhận và chuyển CSKH hỗ trợ ngay."
            elif intent == "greeting":
                reply = "Chào bạn 👋 Bạn đang muốn tìm tour nghỉ dưỡng, khám phá hay đi chill cuối tuần vậy?"
            elif intent in {"tour_search", "recommendation", "tour_comparison", "other"}:
                reply, matched_tours = self._build_reply_from_tours(intent, entities, user_message)
                suggested_tours = [self._tour_to_summary(tour) for tour in matched_tours]

                if intent == "other" and not any([entities.destination, entities.budget, entities.days, entities.travel_group]):
                    reply = (
                        "Mình có thể giúp bạn tìm tour, so sánh tour, hoặc hỗ trợ booking. "
                        "Bạn cho mình ngân sách, số ngày hoặc điểm đến để mình gợi ý sát hơn nhé."
                    )
            else:
                reply = "Mình có thể hỗ trợ bạn tìm tour, so sánh, kiểm tra booking hoặc xử lý phản hồi. Bạn muốn bắt đầu từ đâu?"

        if not suggested_tours and intent in {"tour_search", "recommendation", "tour_comparison"}:
            matched_tours = self.search_tours(entities, user_message)
            suggested_tours = [self._tour_to_summary(tour) for tour in matched_tours[:5]]

        if not reply and tool_calls:
            reply = ""

        return ChatResponse(
            reply=reply,
            intent=intent,
            sentiment=sentiment,
            suggested_tours=suggested_tours,
            suggested_questions=self._suggestions_for(intent, entities),
            extracted_entities=entities,
            tool_calls=tool_calls,
            requires_human_support=requires_human_support,
            metadata=ChatMetadata(processing_time_ms=1, llm_used="mock-rules", tokens_used=None),
        )


mock_travel_assistant = MockTravelAssistant()