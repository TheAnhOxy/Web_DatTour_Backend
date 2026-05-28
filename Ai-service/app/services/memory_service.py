import json
import redis.asyncio as redis
from app.core.config import settings
from typing import List
from app.models.chat import ChatMessage

class MemoryService:
    def __init__(self):
        self.redis_client = None

    async def connect(self):
        print(f"Connecting to Redis at {settings.REDIS_URL}...")
        try:
            self.redis_client = redis.from_url(
                settings.REDIS_URL,
                decode_responses=True,
                socket_connect_timeout=3,
                socket_timeout=3,
            )
            await self.redis_client.ping()
            print("Connected to Redis!")
        except Exception as e:
            print(f"Error connecting to Redis: {e}")
            self.redis_client = None

    async def disconnect(self):
        if self.redis_client:
            print("Closing Redis connection...")
            await self.redis_client.aclose()
            print("Redis connection closed!")

    async def get_history(self, session_id: str, limit: int = 10) -> List[ChatMessage]:
        if not self.redis_client:
            return []
        key = f"chat_history:{session_id}"
        try:
            # Lấy tối đa `limit` tin nhắn gần nhất
            raw_msgs = await self.redis_client.lrange(key, -limit, -1)
            history = []
            for msg in raw_msgs:
                data = json.loads(msg)
                history.append(ChatMessage(**data))
            return history
        except Exception as e:
            print(f"Error getting Redis history: {e}")
            return []

    async def add_message(self, session_id: str, message: ChatMessage):
        if not self.redis_client:
            return
        key = f"chat_history:{session_id}"
        try:
            await self.redis_client.rpush(key, message.model_dump_json())
            # Hết hạn memory sau 24h không tương tác
            await self.redis_client.expire(key, 86400)
        except Exception as e:
            print(f"Error writing Redis message: {e}")

memory_service = MemoryService()
