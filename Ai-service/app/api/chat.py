from fastapi import APIRouter
from app.models.chat import ChatRequest, ChatResponse
from app.services.rag_service import rag_service
from app.services.gemini_service import gemini_service

router = APIRouter(prefix="/api/chat", tags=["chatbot"])

@router.post("/", response_model=ChatResponse)
async def chat_with_bot(request: ChatRequest):
    """
    Endpoint for Chatbot AI.
    1. Receive user message
    2. Search for relevant tours using RAG
    3. Generate reply using Gemini
    """
    user_message = request.message
    
    # 1. RAG: Retrieve context from Database
    context = await rag_service.search_tours_in_mongo(user_message)
    
    # 2. Gemini: Generate response
    reply = await gemini_service.chat_with_travel_assistant(
        user_message=user_message,
        chat_history=request.history,
        retrieved_tours_context=context
    )
    
    return ChatResponse(reply=reply)
