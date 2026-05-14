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
        self.redis_client = redis.from_url(settings.REDIS_URL, decode_responses=True)
        print("Connected to Redis!")

    async def disconnect(self):
        if self.redis_client:
            print("Closing Redis connection...")
            await self.redis_client.aclose()
            print("Redis connection closed!")

    async def get_history(self, session_id: str, limit: int = 10) -> List[ChatMessage]:
        if not self.redis_client:
            return []
        key = f"chat_history:{session_id}"
        # Lấy tối đa `limit` tin nhắn gần nhất
        raw_msgs = await self.redis_client.lrange(key, -limit, -1)
        history = []
        for msg in raw_msgs:
            data = json.loads(msg)
            history.append(ChatMessage(**data))
        return history

    async def add_message(self, session_id: str, message: ChatMessage):
        if not self.redis_client:
            return
        key = f"chat_history:{session_id}"
        await self.redis_client.rpush(key, message.model_dump_json())
        # Hết hạn memory sau 24h không tương tác
        await self.redis_client.expire(key, 86400)

memory_service = MemoryService()
