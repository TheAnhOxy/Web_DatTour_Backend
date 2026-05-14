import httpx
from typing import Dict, Any, Callable
import inspect

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
    if "123" in booking_id:
        return f"Đơn hàng {booking_id} của quý khách đã được THANH TOÁN THÀNH CÔNG và ĐÃ ĐƯỢC XÁC NHẬN. Hướng dẫn viên sẽ liên hệ trước ngày đi 1 ngày."
    return f"Không tìm thấy đơn hàng nào có mã {booking_id}. Quý khách vui lòng kiểm tra lại."

@tool_registry.register
def get_current_weather(location: str) -> str:
    """
    Sử dụng tool này khi người dùng hỏi về thời tiết hiện tại ở một địa điểm du lịch.
    Args:
        location: Tên thành phố hoặc địa điểm du lịch (ví dụ: Đà Lạt, Phú Quốc).
    """
    # Mock data
    return f"Thời tiết tại {location} hiện tại khoảng 22-26 độ C, trời nắng ráo, cực kỳ lý tưởng để đi tham quan chụp ảnh."
