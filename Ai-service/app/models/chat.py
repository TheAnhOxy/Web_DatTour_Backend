from pydantic import BaseModel
from typing import List, Optional, Dict, Any, Literal

class TourSummary(BaseModel):
    id: str
    title: str
    location: str
    price: float
    duration_days: int
    rating: float
    tags: List[str]
    thumbnail_url: Optional[str]

class SuggestedQuestion(BaseModel):
    question: str

class ToolCall(BaseModel):
    tool_name: str
    arguments: Dict[str, Any]
    status: str # Literal["success", "failed", "pending"]

class ChatMetadata(BaseModel):
    processing_time_ms: Optional[int]
    llm_used: Optional[str]
    tokens_used: Optional[int]

class ChatMessage(BaseModel):
    role: str # 'user' or 'model'
    content: str

class ChatRequest(BaseModel):
    session_id: str = "default_session"
    message: str
    history: Optional[List[ChatMessage]] = []

class ExtractedEntities(BaseModel):
    destination: Optional[str] = None
    budget: Optional[float] = None
    days: Optional[int] = None
    travel_group: Optional[str] = None
    booking_id: Optional[str] = None

class ChatResponse(BaseModel):
    reply: str
    intent: str # "tour_search", "tour_comparison", "booking_support", "complaint", "greeting", "other"
    sentiment: str # "positive", "neutral", "negative"
    suggested_tours: List[TourSummary]
    suggested_questions: List[SuggestedQuestion]
    extracted_entities: ExtractedEntities
    tool_calls: List[ToolCall]
    requires_human_support: bool
    metadata: Optional[ChatMetadata]
    # Optional opinion string and confidence score (0.0-1.0)
    opinion: Optional[str] = None
    confidence: Optional[float] = None
