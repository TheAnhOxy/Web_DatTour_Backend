import json
import os
import threading
from typing import Dict, Any

_lock = threading.Lock()

class ConversationAnalytics:
    """Simple in-memory analytics with JSONL persistence for quick observability.

    Tracks per-session arrays and can flush summaries to a JSONL file.
    """

    def __init__(self, storage_path: str = None):
        self.sessions: Dict[str, Dict[str, Any]] = {}
        base = storage_path or os.path.join(os.getcwd(), "Ai-service", "analytics")
        os.makedirs(base, exist_ok=True)
        self.outfile = os.path.join(base, "analytics_events.jsonl")

    def _ensure(self, session_id: str):
        if session_id not in self.sessions:
            self.sessions[session_id] = {
                "intent_history": [],
                "negative_score_history": [],
                "response_lengths": [],
                "cta_count": 0,
                "fallback_count": 0,
                "tool_calls": 0,
                "events": [],
            }

    def record_event(self, session_id: str, event: Dict[str, Any]):
        """Record a raw event (will be persisted).

        Event should be JSON-serializable.
        """
        self._ensure(session_id)
        with _lock:
            self.sessions[session_id]["events"].append(event)
            # append to file for durable log
            try:
                with open(self.outfile, "a", encoding="utf-8") as f:
                    f.write(json.dumps({"session_id": session_id, **event}, ensure_ascii=False) + "\n")
            except Exception:
                pass

    def record_intent(self, session_id: str, intent: str, confidence: float = None):
        self._ensure(session_id)
        with _lock:
            self.sessions[session_id]["intent_history"].append({"intent": intent, "confidence": confidence})

    def record_negative_score(self, session_id: str, score: float):
        self._ensure(session_id)
        with _lock:
            self.sessions[session_id]["negative_score_history"].append(score)

    def record_response_metrics(self, session_id: str, reply: str, suggested_questions_count: int, is_fallback: bool, tool_calls_count: int, extracted_entities: Dict[str, Any], confidence: float = None):
        self._ensure(session_id)
        words = len(reply.split()) if reply else 0
        useful_entities = sum(1 for v in (extracted_entities or {}).values() if v)
        density = words / useful_entities if useful_entities else words
        with _lock:
            self.sessions[session_id]["response_lengths"].append(words)
            self.sessions[session_id]["cta_count"] += suggested_questions_count
            self.sessions[session_id]["fallback_count"] += 1 if is_fallback else 0
            self.sessions[session_id]["tool_calls"] += tool_calls_count

            summary = {
                "event": "response_metrics",
                "reply_word_count": words,
                "useful_entities": useful_entities,
                "response_density": density,
                "suggested_questions_count": suggested_questions_count,
                "is_fallback": is_fallback,
                "tool_calls_count": tool_calls_count,
                "confidence": confidence,
            }
            # persist
            try:
                with open(self.outfile, "a", encoding="utf-8") as f:
                    f.write(json.dumps({"session_id": session_id, **summary}, ensure_ascii=False) + "\n")
            except Exception:
                pass

    def get_session_summary(self, session_id: str) -> Dict[str, Any]:
        self._ensure(session_id)
        s = self.sessions[session_id]
        avg_len = sum(s["response_lengths"]) / len(s["response_lengths"]) if s["response_lengths"] else 0
        avg_negative = sum(s["negative_score_history"]) / len(s["negative_score_history"]) if s["negative_score_history"] else 0
        return {
            "intent_count": len(s["intent_history"]),
            "avg_response_length": avg_len,
            "avg_negative_score": avg_negative,
            "cta_count": s["cta_count"],
            "fallback_count": s["fallback_count"],
            "tool_calls": s["tool_calls"],
        }


analytics_service = ConversationAnalytics()
