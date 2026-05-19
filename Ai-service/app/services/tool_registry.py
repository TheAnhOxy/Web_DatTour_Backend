import httpx
from typing import Dict, Any, Callable
import inspect

MOCK_BOOKINGS: Dict[str, Dict[str, Any]] = {
    "BK123": {
        "booking_id": "BK123",
        "status": "confirmed",
        "customer_name": "Nguyen Van A",
        "tour_name": "Tour Đà Lạt 3N2D",
        "departure_date": "2026-06-28",
        "amount": 6900000,
    },
    "BK456": {
        "booking_id": "BK456",
        "status": "pending_payment",
        "customer_name": "Tran Thi B",
        "tour_name": "Tour Nha Trang 4N3D",
        "departure_date": "2026-07-05",
        "amount": 5800000,
    },
    "BK789": {
        "booking_id": "BK789",
        "status": "cancelled",
        "customer_name": "Le Van C",
        "tour_name": "Tour Phú Quốc 4N3D",
        "departure_date": "2026-07-12",
        "amount": 8900000,
    },
}

MOCK_SUPPORT_TICKETS: list[Dict[str, Any]] = []

class ToolRegistry:
    def __init__(self):
        self.tools: Dict[str, Callable] = {}
        
    def register(self, func: Callable):
        self.tools[func.__name__] = func
        return func

    async def execute_tool(self, tool_name: str, **kwargs) -> Any:
        if tool_name not in self.tools:
            return f"Error: Tool {tool_name} not found."
            
        func = self.tools[tool_name]
        try:
            print(f"Executing tool [{tool_name}] with args: {kwargs}")
            if inspect.iscoroutinefunction(func):
                return await func(**kwargs)
            return func(**kwargs)
        except Exception as e:
            return f"Error executing tool {tool_name}: {str(e)}"

    def get_registered_functions(self):
        return list(self.tools.values())

tool_registry = ToolRegistry()

# ----------------- ĐỊNH NGHĨA CÁC TOOLS Ở ĐÂY -----------------

@tool_registry.register
async def check_booking_status(booking_id: str) -> str:
    """
    Sử dụng tool này khi người dùng muốn kiểm tra trạng thái của một đơn đặt tour (booking).
    Args:
        booking_id: Mã đơn hàng (ví dụ: BK123, 12345). Bắt buộc phải có.
    """
    # Trong thực tế: Gọi sang Booking Service
    # async with httpx.AsyncClient() as client:
    #     res = await client.get(f"http://booking-service:8084/api/bookings/{booking_id}")
    
    # Mock data
    normalized_id = booking_id.strip().upper()
    booking = MOCK_BOOKINGS.get(normalized_id)
    if booking:
        return (
            f"Đơn hàng {normalized_id} của quý khách đang ở trạng thái {booking['status']}. "
            f"Tour: {booking['tour_name']}. Ngày khởi hành: {booking['departure_date']}."
        )
    return f"Không tìm thấy đơn hàng nào có mã {booking_id}. Quý khách vui lòng kiểm tra lại."


@tool_registry.register
async def cancel_booking(booking_id: str, reason: str = "") -> str:
    """
    Sử dụng tool này khi người dùng muốn hủy booking đã đặt.
    Args:
        booking_id: Mã đơn hàng.
        reason: Lý do hủy.
    """
    normalized_id = booking_id.strip().upper()
    booking = MOCK_BOOKINGS.get(normalized_id)
    if not booking:
        return f"Không tìm thấy đơn hàng nào có mã {booking_id}."
    if booking["status"] == "cancelled":
        return f"Đơn hàng {normalized_id} đã được hủy trước đó."

    booking["status"] = "cancelled"
    reason_text = f" Lý do: {reason}." if reason else ""
    return f"Đơn hàng {normalized_id} đã được hủy thành công.{reason_text}"


@tool_registry.register
async def create_support_ticket(booking_id: str = "", issue_type: str = "general", description: str = "") -> str:
    """
    Sử dụng tool này khi cần tạo ticket hỗ trợ cho khách hàng.
    Args:
        booking_id: Mã booking liên quan, nếu có.
        issue_type: Loại vấn đề.
        description: Mô tả ngắn.
    """
    ticket_id = f"TICK-{len(MOCK_SUPPORT_TICKETS) + 1:04d}"
    ticket = {
        "ticket_id": ticket_id,
        "booking_id": booking_id,
        "issue_type": issue_type,
        "description": description,
        "status": "open",
    }
    MOCK_SUPPORT_TICKETS.append(ticket)
    booking_part = f" cho booking {booking_id}" if booking_id else ""
    return f"Đã tạo ticket hỗ trợ {ticket_id}{booking_part}. Bộ phận CSKH sẽ liên hệ sớm nhất có thể."

@tool_registry.register
def get_current_weather(location: str) -> str:
    """
    Sử dụng tool này khi người dùng hỏi về thời tiết hiện tại ở một địa điểm du lịch.
    Args:
        location: Tên thành phố hoặc địa điểm du lịch (ví dụ: Đà Lạt, Phú Quốc).
    """
    # Mock data
    return f"Thời tiết tại {location} hiện tại khoảng 22-26 độ C, trời nắng ráo, cực kỳ lý tưởng để đi tham quan chụp ảnh."
