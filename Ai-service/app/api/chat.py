from fastapi import APIRouter
from app.models.chat import ChatRequest, ChatResponse, ChatMessage, ToolCall, ChatMetadata, SuggestedQuestion, ExtractedEntities
from app.services.rag_service import rag_service
from app.services.gemini_service import gemini_service
from app.services.memory_service import memory_service
from app.services.enhanced_memory_service import memory_service as enhanced_memory_service
from app.services.conversation_fsm import ConversationFSM
from app.services.intent_classifier import IntentClassifier
from app.services.response_formatter import format_chat_response
from app.services.conversation_analytics import analytics_service

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
    
    # 2. Load enhanced conversation state and classifier early (for negative shortcut)
    enhanced_state = await enhanced_memory_service.load_state(session_id)
    fsm = ConversationFSM()
    classifier = IntentClassifier()

    # Quick negative scoring to short-circuit complaint flows before hitting LLM
    try:
        neg_score = classifier.detect_negative_score(user_message)
    except Exception:
        neg_score = 0.0

    if neg_score >= 0.75:
        # Create support ticket via tool and return empathetic response
        import re
        from app.services.tool_registry import tool_registry
        # derive matched rules from keyword classifier for logging
        try:
            kw_result = classifier._classify_with_keywords(user_message)
            matched_rules = kw_result.matched_rules or []
        except Exception:
            matched_rules = []
        booking_id_match = re.search(r"(BK\d{3,})", user_message.upper())
        booking_id = booking_id_match.group(1) if booking_id_match else ""

        tool = ToolCall(
            tool_name="create_support_ticket",
            arguments={"bookingCode": booking_id, "booking_id": booking_id, "issue_type": "complaint", "description": user_message},
            status="pending",
        )

        # execute immediately
        res = await tool_registry.execute_tool(tool.tool_name, **tool.arguments)

        # Log debug event for observability
        debug_event = {
            "event": "complaint_shortcut",
            "intent": "complaint",
            "negative_score": neg_score,
            "matched_rules": matched_rules,
            "shortcut_triggered": True,
        }
        try:
            analytics_service.record_event(session_id, debug_event)
        except Exception:
            pass

        reply_text = (
            "Mình rất tiếc vì trải nghiệm đó đã làm bạn thất vọng 😔\n\n"
            "• Mình đã ghi nhận phản hồi của bạn\n"
            "• CSKH sẽ kiểm tra và hỗ trợ sớm nhất\n\n"
            "Bạn có thể cho mình thêm mã booking hoặc ngày đi để mình hỗ trợ nhanh hơn nhé?"
        )
        # Build response
        response = ChatResponse(
            reply=reply_text,
            intent="complaint",
            sentiment="negative",
            suggested_tours=[],
            suggested_questions=[SuggestedQuestion(question="Tôi muốn kết nối với nhân viên hỗ trợ ngay")],
            extracted_entities=ExtractedEntities(destination=None, budget=None, days=None, travel_group=None),
            tool_calls=[ToolCall(tool_name=tool.tool_name, arguments=tool.arguments, status="success")],
            requires_human_support=True,
            metadata=ChatMetadata(processing_time_ms=10, llm_used="none", tokens_used=None),
            opinion=None,
            confidence=neg_score,
        )

        # Save to memory and escalate
        await memory_service.add_message(session_id, ChatMessage(role="model", content=response.reply))
        from app.services.kafka_service import kafka_service
        await kafka_service.send_support_escalation(session_id=session_id, user_message=user_message, intent=response.intent)
        return response

    # 3. Extract Entities via Gemini / extractor
    entities = await gemini_service.extract_entities(user_message, history)

    try:
        entities_dict = entities.model_dump() if hasattr(entities, 'model_dump') else entities.dict()
    except Exception:
        entities_dict = {}

    # 4. Classify intent (pass negative flag)
    classification = await classifier.classify(user_message, entities_dict, has_negative_sentiment=(neg_score>0.4))
    intent_str = classification.intent.value if hasattr(classification.intent, 'value') else str(classification.intent)

    # Determine conversation mode and tone
    current_mode = getattr(enhanced_state, 'conversation_mode', None)
    next_mode = await fsm.determine_mode(intent_str, current_mode)
    mode_prompt_adj = fsm.get_system_prompt_adjustment(next_mode)
    tone = fsm.get_tone(next_mode)

    # 5. RAG: Retrieve context from Search-Service Adapter using extracted entities
    context = await rag_service.search_tours_via_adapter(entities)
    
    # 4. Gemini: Generate Structured Response (Turn 1)
    response: ChatResponse = await gemini_service.chat_with_travel_assistant(
        user_message=user_message,
        chat_history=history,
        retrieved_tours_context=context,
        extracted_entities=entities,
        mode_prompt_adjustment=mode_prompt_adj,
        tone=tone,
        observed_intent=intent_str,
        observed_confidence=classification.confidence,
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
            tool_results=tool_results_str,
            mode_prompt_adjustment=mode_prompt_adj,
            tone=tone,
            observed_intent=intent_str,
            observed_confidence=classification.confidence,
        )
        # Bắt buộc gán lại tool_calls với status success để FE biết Tool nào đã chạy
        response.tool_calls = old_tool_calls
    
    # Apply response formatting for product UX, then save
    try:
        response = format_chat_response(response, mode=next_mode, tone=tone)
    except Exception:
        pass

    await memory_service.add_message(session_id, ChatMessage(role="model", content=response.reply))
    try:
        tour_results = []
        for t in response.suggested_tours or []:
            try:
                tour_results.append(t.model_dump())
            except Exception:
                try:
                    tour_results.append(t.dict())
                except Exception:
                    pass
        await enhanced_memory_service.update_context(
            session_id=session_id,
            intent=intent_str,
            entities=entities_dict,
            tour_results=tour_results,
            conversation_mode=next_mode,
        )
    except Exception:
        pass
    # Record analytics: intent, negative score, response metrics
    try:
        analytics_service.record_intent(session_id, intent_str, classification.confidence if 'classification' in locals() else None)
        analytics_service.record_negative_score(session_id, neg_score)
        suggested_q_count = len(response.suggested_questions) if getattr(response, 'suggested_questions', None) else 0
        is_fallback = not bool(context)
        tool_calls_count = len(response.tool_calls) if getattr(response, 'tool_calls', None) else 0
        analytics_service.record_response_metrics(
            session_id=session_id,
            reply=response.reply,
            suggested_questions_count=suggested_q_count,
            is_fallback=is_fallback,
            tool_calls_count=tool_calls_count,
            extracted_entities=entities_dict,
            confidence=getattr(response, 'confidence', None),
        )
    except Exception:
        pass
    
    # 6. Support Escalation via Kafka
    if response.requires_human_support or response.sentiment == "negative":
        from app.services.kafka_service import kafka_service
        await kafka_service.send_support_escalation(
            session_id=session_id,
            user_message=user_message,
            intent=response.intent
        )
        
    return response
