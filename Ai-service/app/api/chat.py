from fastapi import APIRouter
from app.models.chat import ChatRequest, ChatResponse, ChatMessage
from app.services.rag_service import rag_service
from app.services.gemini_service import gemini_service
from app.services.memory_service import memory_service

router = APIRouter(prefix="/api/chat", tags=["chatbot"])

@router.post("/", response_model=ChatResponse)
async def chat_with_bot(request: ChatRequest):
    """
    Endpoint for Chatbot AI.
    """
    user_message = request.message
    session_id = request.session_id
    
    # 1. Memory: Get short-term history from Redis
    history = await memory_service.get_history(session_id, limit=10)
    
    # Save user message to memory
    await memory_service.add_message(session_id, ChatMessage(role="user", content=user_message))
    
    # 2. Extract Entities via Gemini
    entities = await gemini_service.extract_entities(user_message, history)
    
    # 3. RAG: Retrieve context from Search-Service Adapter using extracted entities
    context = await rag_service.search_tours_via_adapter(entities)
    
    # 4. Gemini: Generate Structured Response (Turn 1)
    response: ChatResponse = await gemini_service.chat_with_travel_assistant(
        user_message=user_message,
        chat_history=history,
        retrieved_tours_context=context,
        extracted_entities=entities
    )
    
    # 5. Handle Tool Calling (If AI requests a tool)
    if response.tool_calls and any(tc.status == "pending" for tc in response.tool_calls):
        from app.services.tool_registry import tool_registry
        import json
        
        old_tool_calls = response.tool_calls
        tool_results_dict = {}
        for tc in old_tool_calls:
            if tc.status == "pending":
                res = await tool_registry.execute_tool(tc.tool_name, **tc.arguments)
                tool_results_dict[tc.tool_name] = res
                tc.status = "success"
                
        # Turn 2: Send results back to Gemini
        tool_results_str = json.dumps(tool_results_dict, ensure_ascii=False)
        response = await gemini_service.chat_with_travel_assistant(
            user_message=user_message,
            chat_history=history,
            retrieved_tours_context=context,
            extracted_entities=entities,
            tool_results=tool_results_str
        )
        # Bắt buộc gán lại tool_calls với status success để FE biết Tool nào đã chạy
        response.tool_calls = old_tool_calls
    
    # Save model response to memory
    await memory_service.add_message(session_id, ChatMessage(role="model", content=response.reply))
    
    # 6. Support Escalation via Kafka
    if response.requires_human_support or response.sentiment == "negative":
        from app.services.kafka_service import kafka_service
        await kafka_service.send_support_escalation(
            session_id=session_id,
            user_message=user_message,
            intent=response.intent
        )
        
    return response
