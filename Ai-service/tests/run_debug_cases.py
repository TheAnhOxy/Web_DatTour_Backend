import asyncio
import json
import sys
from typing import List

from app.services.gemini_service import gemini_service
from app.services.intent_classifier import IntentClassifier
from app.services.conversation_fsm import ConversationFSM
from app.services.enhanced_memory_service import memory_service as enhanced_memory_service
from app.services.memory_service import memory_service
from app.api.chat import chat_with_bot
from app.models.chat import ChatRequest


CASES = {
    "recommendation": [
        "Tháng 11 nên đi đâu?",
        "Tôi thích thiên nhiên",
        "Đi đâu đẹp?",
    ],
    "complaint": [
        "Tour hôm trước quá tệ",
        "CSKH phản hồi quá chậm",
        "Khách sạn rất bẩn",
    ],
    "greeting_overfit": [
        "Tôi có 10 triệu đi Nhật",
        "Đi biển budget 5tr",
    ]
}


async def run_one(session_id: str, message: str):
    # load short-term history
    history = await memory_service.get_history(session_id, limit=10)

    # extract entities
    try:
        entities = await gemini_service.extract_entities(message, history)
        try:
            entities_dict = entities.model_dump() if hasattr(entities, 'model_dump') else entities.dict()
        except Exception:
            entities_dict = {}
    except Exception:
        entities = None
        entities_dict = {}

    # enhanced state
    enhanced_state = await enhanced_memory_service.load_state(session_id)

    # classifier
    classifier = IntentClassifier()
    neg_score = 0.0
    try:
        neg_score = classifier.detect_negative_score(message)
    except Exception:
        neg_score = 0.0

    classification = await classifier.classify(message, entities_dict, has_negative_sentiment=(neg_score>0.4))

    intent_str = classification.intent.value if hasattr(classification.intent, 'value') else str(classification.intent)

    # FSM
    fsm = ConversationFSM()
    current_mode = getattr(enhanced_state, 'conversation_mode', None)
    next_mode = await fsm.determine_mode(intent_str, current_mode)

    # final chat response (invoke full pipeline)
    req = ChatRequest(session_id=session_id, message=message)
    try:
        final = await chat_with_bot(req)
    except Exception as e:
        final = None

    debug = {
        "message": message,
        "intent": intent_str,
        "confidence": getattr(classification, 'confidence', None),
        "matched_rules": getattr(classification, 'matched_rules', None),
        "why": getattr(classification, 'why', None),
        "negative_score": neg_score,
        "final_mode": next_mode,
        "final_response_intent": getattr(final, 'intent', None) if final else None,
        "final_response_confidence": getattr(final, 'confidence', None) if final else None,
        "final_reply": getattr(final, 'reply', None) if final else None
    }

    print(json.dumps(debug, ensure_ascii=False, indent=2))


def main():
    loop = asyncio.get_event_loop()
    tasks: List = []
    i = 1
    for group, msgs in CASES.items():
        for m in msgs:
            sid = f"debug_{group}_{i}"
            tasks.append(run_one(sid, m))
            i += 1

    loop.run_until_complete(asyncio.gather(*tasks))


if __name__ == "__main__":
    # ensure Ai-service path is importable when running directly
    if 'Ai-service' not in sys.path:
        sys.path.insert(0, 'Ai-service')
    main()
