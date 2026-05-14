import json
from aiokafka import AIOKafkaProducer
from app.core.config import settings

class KafkaService:
    def __init__(self):
        self.producer = None

    async def connect(self):
        print(f"Connecting to Kafka at {settings.KAFKA_BOOTSTRAP_SERVERS}...")
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            await self.producer.start()
            print("Connected to Kafka!")
        except Exception as e:
            print(f"Error connecting to Kafka (make sure Docker Kafka is running): {e}")
            self.producer = None

    async def disconnect(self):
        if self.producer:
            print("Closing Kafka connection...")
            await self.producer.stop()
            print("Kafka connection closed!")

    async def send_support_escalation(self, session_id: str, user_message: str, intent: str):
        if not self.producer:
            print("Kafka producer not available, skipping event publish.")
            return

        topic = "support_ticket_events"
        event_payload = {
            "session_id": session_id,
            "user_id": "GUEST", # Sau này có Auth thì lấy ID thật
            "title": "AI Escalation: Khách hàng phàn nàn",
            "category": "COMPLAINT",
            "content": f"Khách hàng cần hỗ trợ. Ý định: {intent}. Tin nhắn cuối: {user_message}",
            "priority": "HIGH",
            "status": "OPEN",
            "source": "AI_CHATBOT"
        }
        try:
            await self.producer.send_and_wait(topic, event_payload)
            print(f"Successfully published escalation event to Kafka topic '{topic}'")
        except Exception as e:
            print(f"Failed to publish to Kafka: {e}")

kafka_service = KafkaService()
