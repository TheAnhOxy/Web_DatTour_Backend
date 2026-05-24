import asyncio
import os
import sys

# Add the project root to sys.path to allow absolute imports
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.join(current_dir, 'Ai-service')
sys.path.append(project_root)

# Set AI_MODE to mock
os.environ['AI_MODE'] = 'mock'
os.environ['DATABASE_URL'] = 'sqlite+aiosqlite:///:memory:' # Dummy DB URL
os.environ['GEMINI_API_KEY'] = '' # Ensure it is empty for mock mode if needed

from app.services.gemini_service import GeminiService
from app.models.chat import ChatMessage, ExtractedEntities

async def run_smoke_test():
    service = GeminiService()
    
    print("--- Test 1: Tour Search ---")
    user_msg1 = 'Tôi muốn đi Đà Lạt 3 ngày dưới 5 triệu'
    history1 = []
    
    entities1 = await service.extract_entities(user_msg1, history1)
    print(f"Entities: {entities1}")
    
    response1 = await service.chat_with_travel_assistant(
        user_message=user_msg1,
        chat_history=history1,
        retrieved_tours_context="Tour Đà Lạt 3N2D",
        extracted_entities=entities1
    )
    
    pass1 = (entities1.destination and "đà lạt" in entities1.destination.lower()) and len(response1.suggested_tours) > 0
    print(f"Response 1: {response1.reply[:100]}...")
    print(f"Suggested Tours count: {len(response1.suggested_tours)}")
    print(f"Test 1 Pass: {pass1}")

    print("\n--- Test 2: Context Preservation ---")
    history2 = [
        ChatMessage(role='user', content=user_msg1),
        ChatMessage(role='assistant', content=response1.reply)
    ]
    user_msg2 = 'Giá bao nhiêu?'
    
    entities2 = await service.extract_entities(user_msg2, history2)
    print(f"Entities: {entities2}")
    
    pass2 = (entities2.destination and "đà lạt" in entities2.destination.lower())
    print(f"Test 2 Pass: {pass2}")

    print("\n--- Test 3: Booking Tool Call ---")
    user_msg3 = 'Kiểm tra booking BK123'
    history3 = []
    entities3 = ExtractedEntities(destination=None, budget=None, days=None, travel_group=None)
    
    response3 = await service.chat_with_travel_assistant(
        user_message=user_msg3,
        chat_history=history3,
        retrieved_tours_context="",
        extracted_entities=entities3
    )
    
    found_check_booking = False
    for tc in response3.tool_calls:
         if "check_booking" in tc.tool_name:
             found_check_booking = True
             break
    
    print(f"Tool Calls: {response3.tool_calls}")
    
    response3_final = await service.chat_with_travel_assistant(
        user_message=user_msg3,
        chat_history=history3,
        retrieved_tours_context="",
        extracted_entities=entities3,
        tool_results="Trạng thái booking BK123: Đã xác nhận"
    )
    
    pass3 = found_check_booking and "xác nhận" in response3_final.reply.lower()
    print(f"Test 3 Pass: {pass3}")

    print("\n--- Summary ---")
    print(f"Test 1: {'PASS' if pass1 else 'FAIL'}")
    print(f"Test 2: {'PASS' if pass2 else 'FAIL'}")
    print(f"Test 3: {'PASS' if pass3 else 'FAIL'}")

if __name__ == '__main__':
    try:
        asyncio.run(run_smoke_test())
    except Exception as e:
        print(f"Runtime Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
