"""
Enhanced Memory Service with Conversation State Management.
Stores not just chat_history, but full conversation context:
- last_intent, last_destination, last_tour_ids, last_filters
- conversation_mode (DISCOVERY, BOOKING_SUPPORT, COMPLAINT, CASUAL)
- user preferences learned over time
"""
import json
import redis.asyncio as redis
from datetime import datetime
from typing import List, Dict, Optional
from pydantic import BaseModel
from app.core.config import settings
from app.models.chat import ChatMessage


class ConversationMode(str):
    """Conversation mode states"""
    DISCOVERY = "DISCOVERY"
    BOOKING_SUPPORT = "BOOKING_SUPPORT"
    COMPLAINT = "COMPLAINT"
    CASUAL = "CASUAL"


class ConversationState(BaseModel):
    """Full conversation state stored in Redis"""
    session_id: str
    
    # Context from current conversation
    last_intent: Optional[str] = None
    last_destination: Optional[str] = None
    last_budget: Optional[float] = None
    last_days: Optional[int] = None
    last_group: Optional[str] = None
    last_booking_id: Optional[str] = None
    
    # Last search results (for multi-turn reference)
    last_tour_ids: List[str] = []
    last_tour_results: List[Dict] = []
    last_filters: Dict = {}
    
    # Conversation mode
    conversation_mode: str = ConversationMode.DISCOVERY
    
    # Chat history
    chat_history: List[Dict] = []
    
    # User profile (learned over time)
    preferred_destinations: List[str] = []
    preferred_groups: List[str] = []
    
    # Timestamps
    created_at: datetime = None
    updated_at: datetime = None
    last_active: datetime = None
    
    def __init__(self, **data):
        super().__init__(**data)
        if self.created_at is None:
            self.created_at = datetime.now()
        if self.updated_at is None:
            self.updated_at = datetime.now()
        if self.last_active is None:
            self.last_active = datetime.now()


class EnhancedMemoryService:
    """
    Enhanced memory service with conversation state tracking.
    Maintains full conversation context for multi-turn interactions.
    """
    
    def __init__(self):
        self.redis_client = None
    
    async def connect(self):
        """Connect to Redis"""
        print(f"Connecting to Redis at {settings.REDIS_URL}...")
        try:
            self.redis_client = redis.from_url(
                settings.REDIS_URL, 
                decode_responses=True,
                socket_connect_timeout=5
            )
            # Test connection
            await self.redis_client.ping()
            print("✅ Connected to Redis!")
        except Exception as e:
            print(f"❌ Redis connection failed: {e}")
            self.redis_client = None
    
    async def disconnect(self):
        """Disconnect from Redis"""
        if self.redis_client:
            print("Closing Redis connection...")
            await self.redis_client.aclose()
            print("Redis connection closed!")
    
    # ========== CONVERSATION STATE MANAGEMENT ==========
    
    async def load_state(self, session_id: str) -> ConversationState:
        """Load full conversation state from Redis"""
        if not self.redis_client:
            return ConversationState(session_id=session_id)
        
        try:
            key = f"state:{session_id}"
            data = await self.redis_client.get(key)
            
            if data:
                state_dict = json.loads(data)
                # Parse datetime strings
                state_dict["created_at"] = datetime.fromisoformat(state_dict["created_at"])
                state_dict["updated_at"] = datetime.fromisoformat(state_dict["updated_at"])
                state_dict["last_active"] = datetime.fromisoformat(state_dict["last_active"])
                return ConversationState(**state_dict)
            else:
                return ConversationState(session_id=session_id)
        except Exception as e:
            print(f"⚠️ Error loading state: {e}")
            return ConversationState(session_id=session_id)
    
    async def save_state(self, state: ConversationState):
        """Save full conversation state to Redis"""
        if not self.redis_client:
            return
        
        try:
            key = f"state:{state.session_id}"
            # Convert to dict with ISO format datetimes
            state_dict = json.loads(state.model_dump_json(default=str))
            await self.redis_client.set(
                key,
                json.dumps(state_dict, ensure_ascii=False),
                ex=86400  # 24h TTL
            )
        except Exception as e:
            print(f"⚠️ Error saving state: {e}")
    
    async def update_context(self, 
                            session_id: str,
                            intent: Optional[str] = None,
                            entities: Optional[Dict] = None,
                            tour_results: Optional[List[Dict]] = None,
                            conversation_mode: Optional[str] = None):
        """Update conversation context after each turn"""
        
        state = await self.load_state(session_id)
        
        # Update basic context
        if intent:
            state.last_intent = intent
        
        if entities:
            if "destination" in entities and entities["destination"]:
                state.last_destination = entities["destination"]
                # Learn destination preference
                if entities["destination"] not in state.preferred_destinations:
                    state.preferred_destinations.append(entities["destination"])
            
            if "budget" in entities and entities["budget"]:
                state.last_budget = entities["budget"]
            
            if "days" in entities and entities["days"]:
                state.last_days = entities["days"]
            
            if "group" in entities and entities["group"]:
                state.last_group = entities["group"]
                # Learn group preference
                if entities["group"] not in state.preferred_groups:
                    state.preferred_groups.append(entities["group"])
            
            if "booking_id" in entities and entities["booking_id"]:
                state.last_booking_id = entities["booking_id"]
        
        # Update tour results
        if tour_results:
            state.last_tour_ids = [t.get("id") for t in tour_results]
            state.last_tour_results = tour_results
        
        # Update conversation mode
        if conversation_mode:
            state.conversation_mode = conversation_mode
        
        # Update timestamps
        state.updated_at = datetime.now()
        state.last_active = datetime.now()
        
        await self.save_state(state)
        return state
    
    # ========== CHAT HISTORY MANAGEMENT ==========
    
    async def get_history(self, session_id: str, limit: int = 10) -> List[ChatMessage]:
        """Get chat history (last `limit` messages)"""
        if not self.redis_client:
            return []
        
        try:
            key = f"chat_history:{session_id}"
            raw_msgs = await self.redis_client.lrange(key, -limit, -1)
            history = []
            for msg in raw_msgs:
                data = json.loads(msg)
                history.append(ChatMessage(**data))
            return history
        except Exception as e:
            print(f"⚠️ Error getting history: {e}")
            return []
    
    async def add_message(self, session_id: str, message: ChatMessage):
        """Add message to chat history"""
        if not self.redis_client:
            return
        
        try:
            key = f"chat_history:{session_id}"
            await self.redis_client.rpush(
                key, 
                message.model_dump_json(default=str)
            )
            # 24h TTL
            await self.redis_client.expire(key, 86400)
        except Exception as e:
            print(f"⚠️ Error adding message: {e}")
    
    # ========== UTILITY METHODS ==========
    
    async def clear_session(self, session_id: str):
        """Clear all session data"""
        if not self.redis_client:
            return
        
        try:
            await self.redis_client.delete(f"state:{session_id}")
            await self.redis_client.delete(f"chat_history:{session_id}")
        except Exception as e:
            print(f"⚠️ Error clearing session: {e}")
    
    async def get_session_summary(self, session_id: str) -> Dict:
        """Get session summary for admin/debugging"""
        state = await self.load_state(session_id)
        history = await self.get_history(session_id, limit=100)
        
        return {
            "session_id": session_id,
            "conversation_mode": state.conversation_mode,
            "last_intent": state.last_intent,
            "last_destination": state.last_destination,
            "last_budget": state.last_budget,
            "last_days": state.last_days,
            "last_group": state.last_group,
            "preferred_destinations": state.preferred_destinations,
            "preferred_groups": state.preferred_groups,
            "message_count": len(history),
            "created_at": state.created_at.isoformat() if state.created_at else None,
            "last_active": state.last_active.isoformat() if state.last_active else None,
        }


# Global instance
memory_service = EnhancedMemoryService()
