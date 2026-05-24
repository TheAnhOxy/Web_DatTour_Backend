import asyncio
import json
import os
from app.models.chat import ChatRequest
from app.api.chat import chat_with_bot


TEST_FILE = os.path.join(os.path.dirname(__file__), "batch_tests.json")
OUT_FILE = os.path.join(os.path.dirname(__file__), "batch_results.jsonl")


async def run_case(i, case):
    msg = case.get("message")
    expected = case.get("expected_intent")
    req = ChatRequest(session_id=f"batch_{i}", message=msg)
    try:
        resp = await chat_with_bot(req)
    except Exception as e:
        return {
            "index": i,
            "message": msg,
            "expected_intent": expected,
            "actual_intent": None,
            "confidence": None,
            "passed": False,
            "error": str(e)
        }

    actual = getattr(resp, "intent", None)
    conf = getattr(resp, "confidence", None)
    reply = getattr(resp, "reply", "") or ""
    suggested_q = len(getattr(resp, "suggested_questions", []) or [])
    tool_calls = len(getattr(resp, "tool_calls", []) or [])

    # simple fallback detection
    is_fallback = "[Mock Fallback]" in reply or (not reply.strip())

    passed = (actual == expected)

    out = {
        "index": i,
        "message": msg,
        "expected_intent": expected,
        "actual_intent": actual,
        "confidence": conf,
        "passed": passed,
        "reply_word_count": len(reply.split()),
        "suggested_questions": suggested_q,
        "tool_calls": tool_calls,
        "is_fallback": is_fallback,
    }

    # persist per-case
    try:
        with open(OUT_FILE, "a", encoding="utf-8") as f:
            f.write(json.dumps(out, ensure_ascii=False) + "\n")
    except Exception:
        pass

    return out


def summarize(results):
    total = len(results)
    passed = sum(1 for r in results if r.get("passed"))
    failed = total - passed

    # intent accuracy
    intent_acc = passed / total if total else 0

    # complaint precision
    complaints = [r for r in results if r.get("actual_intent") == "complaint"]
    tp = sum(1 for r in complaints if r.get("expected_intent") == "complaint")
    complaint_precision = tp / len(complaints) if complaints else None

    fallback_rate = sum(1 for r in results if r.get("is_fallback")) / total if total else 0

    avg_words = sum(r.get("reply_word_count", 0) for r in results) / total if total else 0
    avg_cta = sum(r.get("suggested_questions", 0) for r in results) / total if total else 0

    return {
        "total_tests": total,
        "passed": passed,
        "failed": failed,
        "intent_accuracy": round(intent_acc, 3),
        "complaint_precision": round(complaint_precision, 3) if complaint_precision is not None else None,
        "fallback_rate": round(fallback_rate, 3),
        "avg_response_words": round(avg_words, 1),
        "avg_cta_count": round(avg_cta, 2),
    }


def main():
    with open(TEST_FILE, encoding="utf-8") as f:
        cases = json.load(f)

    loop = asyncio.get_event_loop()
    tasks = [run_case(i + 1, c) for i, c in enumerate(cases)]
    results = loop.run_until_complete(asyncio.gather(*tasks))

    summary = summarize(results)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    # clear old results
    try:
        if os.path.exists(OUT_FILE):
            os.remove(OUT_FILE)
    except Exception:
        pass
    main()
