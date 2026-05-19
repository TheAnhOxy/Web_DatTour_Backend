"""
Conversation Mode FSM (Finite State Machine).
Manages conversation state transitions and mode-specific response adjustments.
"""
from enum import Enum
from typing import Dict, Optional
from pydantic import BaseModel


class ConversationMode(str, Enum):
    """Possible conversation modes"""
    DISCOVERY = "DISCOVERY"              # User exploring options
    BOOKING_SUPPORT = "BOOKING_SUPPORT"  # Processing booking/payment
    COMPLAINT = "COMPLAINT"               # Handling issues
    CASUAL = "CASUAL"                     # General chat


class ModeContext(BaseModel):
    """Context information for a conversation mode"""
    mode: ConversationMode
    system_prompt_adjustment: str
    tone: str
    expected_actions: list
    allowed_next_modes: list


class ConversationFSM:
    """
    Finite State Machine for conversation management.
    Ensures logical flow and mode-appropriate responses.
    """
    
    # Define mode characteristics and transitions
    MODE_DEFINITIONS = {
        ConversationMode.DISCOVERY: ModeContext(
            mode=ConversationMode.DISCOVERY,
            system_prompt_adjustment=(
                "Hãy gợi ý tour phù hợp dựa trên nhu cầu. "
                "Giải thích lý do tại sao tour phù hợp. "
                "Nếu user cần thêm thông tin, hãy hỏi chi tiết."
            ),
            tone="helpful, enthusiastic, informative",
            expected_actions=["search_tours", "recommend", "compare"],
            allowed_next_modes=[
                ConversationMode.BOOKING_SUPPORT,
                ConversationMode.COMPLAINT,
                ConversationMode.CASUAL
            ]
        ),
        ConversationMode.BOOKING_SUPPORT: ModeContext(
            mode=ConversationMode.BOOKING_SUPPORT,
            system_prompt_adjustment=(
                "Hãy hỗ trợ booking và thanh toán. "
                "Cung cấp thông tin rõ ràng về quy trình. "
                "Nếu cần, hãy hướng dẫn từng bước."
            ),
            tone="professional, clear, supportive",
            expected_actions=["check_booking", "process_payment", "cancel_booking"],
            allowed_next_modes=[
                ConversationMode.DISCOVERY,
                ConversationMode.COMPLAINT,
            ]
        ),
        ConversationMode.COMPLAINT: ModeContext(
            mode=ConversationMode.COMPLAINT,
            system_prompt_adjustment=(
                "Hãy lắng nghe và ghi nhận phản hồi. "
                "Tỏ ra đồng cảm và xin lỗi vì trải nghiệm không tốt. "
                "Đề xuất giải pháp hoặc escalate ngay cho CSKH. "
                "LƯU Ý: Nếu user muốn hoàn tiền/compensation, bắt buộc escalate."
            ),
            tone="empathetic, professional, urgent",
            expected_actions=["escalate_support", "create_ticket", "offer_compensation"],
            allowed_next_modes=[
                ConversationMode.DISCOVERY,
            ]
        ),
        ConversationMode.CASUAL: ModeContext(
            mode=ConversationMode.CASUAL,
            system_prompt_adjustment=(
                "Hãy trò chuyện tự nhiên, thân thiện, vui vẻ. "
                "Có thể chia sẻ kinh nghiệm, ý kiến về du lịch. "
                "Nếu user quan tâm tour, hãy chuyển sang DISCOVERY mode."
            ),
            tone="friendly, casual, warm",
            expected_actions=["chat", "share_info"],
            allowed_next_modes=[
                ConversationMode.DISCOVERY,
                ConversationMode.BOOKING_SUPPORT,
            ]
        ),
    }
    
    # Intent to mode mapping
    INTENT_TO_MODE = {
        "greeting": ConversationMode.CASUAL,
        "casual_chat": ConversationMode.CASUAL,
        "tour_search": ConversationMode.DISCOVERY,
        "recommendation": ConversationMode.DISCOVERY,
        "comparison": ConversationMode.DISCOVERY,
        "booking_support": ConversationMode.BOOKING_SUPPORT,
        "complaint": ConversationMode.COMPLAINT,
        "other": ConversationMode.CASUAL,
    }
    
    async def determine_mode(self, 
                            intent: str, 
                            current_mode: Optional[str] = None) -> str:
        """
        Determine next conversation mode based on intent.
        Respects state machine transitions.
        """
        
        # Get target mode from intent
        target_mode = self.INTENT_TO_MODE.get(intent, ConversationMode.CASUAL)
        
        # If no current mode, use target
        if not current_mode:
            return target_mode
        
        # If same mode, stay
        if current_mode == target_mode:
            return current_mode
        
        # Check if transition is allowed
        current_context = self.MODE_DEFINITIONS.get(current_mode)
        if current_context and target_mode in current_context.allowed_next_modes:
            return target_mode
        
        # If transition not allowed, stay in current mode
        return current_mode
    
    def get_mode_context(self, mode: str) -> ModeContext:
        """Get context information for a mode"""
        return self.MODE_DEFINITIONS.get(
            mode, 
            self.MODE_DEFINITIONS[ConversationMode.CASUAL]
        )
    
    def get_system_prompt_adjustment(self, mode: str) -> str:
        """Get system prompt adjustment for a mode"""
        context = self.get_mode_context(mode)
        return context.system_prompt_adjustment
    
    def get_tone(self, mode: str) -> str:
        """Get response tone for a mode"""
        context = self.get_mode_context(mode)
        return context.tone
    
    def get_allowed_actions(self, mode: str) -> list:
        """Get expected actions for a mode"""
        context = self.get_mode_context(mode)
        return context.expected_actions


# ========== TRANSITION DIAGRAM ==========
"""
State transition diagram:

                    DISCOVERY
                   /    |    \
                  /     |     \
            CASUAL   BOOKING   COMPLAINT
              / \     /  \       |
             /   \   /    \      |
        CASUAL---+---+------+-----+
        
        RULES:
        - Any mode can go to CASUAL (general chat)
        - DISCOVERY can go to BOOKING_SUPPORT or COMPLAINT
        - BOOKING_SUPPORT can go back to DISCOVERY or COMPLAINT
        - COMPLAINT can go back to DISCOVERY
"""


# ========== TESTS ==========
if __name__ == "__main__":
    fsm = ConversationFSM()
    
    test_cases = [
        {
            "current": ConversationMode.DISCOVERY,
            "intent": "booking_support",
            "expected": ConversationMode.BOOKING_SUPPORT
        },
        {
            "current": ConversationMode.DISCOVERY,
            "intent": "complaint",
            "expected": ConversationMode.COMPLAINT
        },
        {
            "current": ConversationMode.BOOKING_SUPPORT,
            "intent": "tour_search",
            "expected": ConversationMode.DISCOVERY
        },
        {
            "current": ConversationMode.COMPLAINT,
            "intent": "casual_chat",
            "expected": ConversationMode.CASUAL
        },
        {
            "current": ConversationMode.CASUAL,
            "intent": "tour_search",
            "expected": ConversationMode.DISCOVERY
        },
    ]
    
    print("🧪 Conversation FSM Tests\n" + "="*50)
    for i, test in enumerate(test_cases, 1):
        import asyncio
        result = asyncio.run(fsm.determine_mode(test["intent"], test["current"]))
        print(f"\n{i}. {test['current']} + {test['intent']}")
        print(f"   Expected: {test['expected']}")
        print(f"   Got: {result}")
        status = "✅" if result == test['expected'] else "❌"
        print(f"   {status}")
        
        if result == test["expected"]:
            context = fsm.get_mode_context(result)
            print(f"   Mode: {context.mode}")
            print(f"   Tone: {context.tone}")
