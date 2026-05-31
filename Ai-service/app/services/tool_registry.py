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
async def check_booking_status(bookingCode: str = None, booking_id: str = None) -> str:
    """
    Sử dụng tool này khi người dùng muốn kiểm tra trạng thái của một đơn đặt tour (booking).
    Args:
        bookingCode: Mã đơn hàng (ví dụ: BK123, BK74089080387848). Ưu tiên dùng trường này.
        booking_id: Mã đơn hàng (ví dụ: BK123, BK74089080387848). Tương đương bookingCode.
    """
    code = bookingCode or booking_id
    if not code:
        return "Lỗi: Không tìm thấy mã đơn đặt tour (bookingCode)."
        
    normalized_id = code.strip().upper()
    url = f"http://localhost:8084/bookings/{normalized_id}"
    
    print(f"Calling Booking-service API for booking status: {url}")
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, timeout=5.0)
            if response.status_code == 200:
                res_json = response.json()
                if res_json.get("status") == 200 and res_json.get("data"):
                    booking = res_json["data"]
                    price_snap = booking.get("priceSnapshot") or {}
                    tour_title = price_snap.get("tourTitle") or "N/A"
                    start_date = price_snap.get("startDate") or "N/A"
                    return (
                        f"Đơn hàng {normalized_id} của quý khách đang ở trạng thái {booking.get('status')}. "
                        f"Tour: {tour_title}. Ngày khởi hành: {start_date}."
                    )
    except Exception as e:
        print(f"Booking-service offline or error: {e}. Falling back to mock data.")

    # Fallback mock data
    booking = MOCK_BOOKINGS.get(normalized_id)
    if booking:
        return (
            f"[Mock Fallback] Đơn hàng {normalized_id} của quý khách đang ở trạng thái {booking['status']}. "
            f"Tour: {booking['tour_name']}. Ngày khởi hành: {booking['departure_date']}."
        )
    return f"Không tìm thấy đơn hàng nào có mã {code}. Quý khách vui lòng kiểm tra lại."


@tool_registry.register
async def cancel_booking(bookingCode: str = None, booking_id: str = None, reason: str = "") -> str:
    """
    Sử dụng tool này khi người dùng muốn hủy booking đã đặt.
    Args:
        bookingCode: Mã đơn hàng. Ưu tiên dùng trường này.
        booking_id: Mã đơn hàng. Tương đương bookingCode.
        reason: Lý do hủy.
    """
    code = bookingCode or booking_id
    if not code:
        return "Lỗi: Không tìm thấy mã đơn đặt tour (bookingCode)."
        
    normalized_id = code.strip().upper()
    url = "http://localhost:8084/bookings/cancel"
    payload = {
        "bookingCode": normalized_id,
        "reason": reason or "Khách hàng yêu cầu hủy qua chatbot"
    }
    
    print(f"Calling Booking-service API to cancel booking: {url} with payload {payload}")
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload, timeout=5.0)
            if response.status_code == 200:
                res_json = response.json()
                if res_json.get("status") == 200:
                    return f"Đơn hàng {normalized_id} đã được hủy thành công. Lý do: {reason or 'Khách hàng yêu cầu'}"
                else:
                    return f"Không thể hủy đơn hàng {code}: {res_json.get('message')}"
    except Exception as e:
        print(f"Booking-service cancel API offline or error: {e}. Falling back to mock data.")

    # Fallback mock data
    booking = MOCK_BOOKINGS.get(normalized_id)
    if not booking:
        return f"Không tìm thấy đơn hàng nào có mã {code}."
    if booking["status"] == "cancelled":
        return f"Đơn hàng {normalized_id} đã được hủy trước đó."

    booking["status"] = "cancelled"
    reason_text = f" Lý do: {reason}." if reason else ""
    return f"[Mock Fallback] Đơn hàng {normalized_id} đã được hủy thành công.{reason_text}"


@tool_registry.register
async def create_support_ticket(bookingCode: str = "", booking_id: str = "", issue_type: str = "general", description: str = "") -> str:
    """
    Sử dụng tool này khi cần tạo ticket hỗ trợ cho khách hàng.
    Args:
        bookingCode: Mã booking liên quan, nếu có.
        booking_id: Mã booking liên quan, nếu có.
        issue_type: Loại vấn đề.
        description: Mô tả ngắn.
    """
    code = bookingCode or booking_id
    url = "http://localhost:8086/supports/tickets"
    category = "BOOKING"
    issue_type_upper = issue_type.upper()
    if any(k in issue_type_upper for k in ["REFUND", "CANCEL", "HOAN"]):
        category = "REFUND"
    elif any(k in issue_type_upper for k in ["ACCOUNT", "USER", "TAI KHOAN"]):
        category = "ACCOUNT"
        
    title = f"Yêu cầu hỗ trợ {issue_type}"
    if code:
        title += f" cho Booking {code}"
        
    content = description or "Khách hàng yêu cầu hỗ trợ qua AI chatbot."
    if code:
        content = f"Booking ID: {code}. {content}"
        
    payload = {
        "userId": 1,
        "title": title,
        "category": category,
        "content": content,
        "priority": "HIGH" if category == "REFUND" else "MEDIUM",
        "status": "OPEN"
    }
    
    print(f"Calling Support-service API to create ticket: {url} with payload {payload}")
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(url, json=payload, timeout=5.0)
            if response.status_code == 200:
                res_json = response.json()
                if res_json.get("status") == 200 and res_json.get("data"):
                    ticket = res_json["data"]
                    ticket_id = ticket.get("id")
                    booking_part = f" cho booking {code}" if code else ""
                    return f"Đã tạo ticket hỗ trợ {ticket_id}{booking_part}. Bộ phận CSKH sẽ liên hệ sớm nhất có thể."
    except Exception as e:
        print(f"Support-service offline or error: {e}. Falling back to mock data.")

    # Fallback mock data
    ticket_id = f"TICK-{len(MOCK_SUPPORT_TICKETS) + 1:04d}"
    ticket = {
        "ticket_id": ticket_id,
        "booking_id": code,
        "issue_type": issue_type,
        "description": description,
        "status": "open",
    }
    MOCK_SUPPORT_TICKETS.append(ticket)
    booking_part = f" cho booking {code}" if code else ""
    return f"[Mock Fallback] Đã tạo ticket hỗ trợ {ticket_id}{booking_part}. Bộ phận CSKH sẽ liên hệ sớm nhất có thể."

@tool_registry.register
def get_current_weather(location: str) -> str:
    """
    Sử dụng tool này khi người dùng hỏi về thời tiết hiện tại ở một địa điểm du lịch.
    Args:
        location: Tên thành phố hoặc địa điểm du lịch (ví dụ: Đà Lạt, Phú Quốc).
    """
    # Mock data
    return f"Thời tiết tại {location} hiện tại khoảng 22-26 độ C, trời nắng ráo, cực kỳ lý tưởng để đi tham quan chụp ảnh."
