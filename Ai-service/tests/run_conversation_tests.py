import asyncio
import json
from app.models.chat import ChatRequest
from app.api.chat import chat_with_bot


TEST_CASES = [
    ("Too verbose", "Cho tôi biết tất cả tour Đà Lạt có giá rẻ nhất. Mô tả chi tiết từng tour, lịch trình, khách sạn, giá và các điều khoản..."),
    ("Complaint", "Tour hôm trước quá tệ, khách sạn dơ và HDV không chuyên. Tôi muốn khiếu nại"),
    ("Ambiguous", "Đi đâu đẹp?"),
    ("Comparison", "So sánh Tour Đà Lạt 3N2D với Tour Nha Trang 4N3D, cái nào đáng tiền hơn?"),
    ("Recommendation", "Mình muốn đi vào tháng 11, thích thiên nhiên, đi với người yêu, budget 7tr"),
    ("Casual", "Bạn thích đi đâu hơn: biển hay núi?"),
    ("Booking check", "Booking BK123 ở đâu?"),
    ("Cancel booking", "Hủy BK456 ngay"),
    ("Typo/slang", "tour dl 3n2d, di bien 5tr"),
    ("Mixed language", "budget 10tr đi Japan")
]


async def run_test_case(i, title, message):
    req = ChatRequest(session_id=f"test_{i}", message=message)
    try:
        resp = await chat_with_bot(req)
    except Exception as e:
        print(f"[{i}] {title} - ERROR: {e}")
        return

    # Print a compact summary of the response
    out = {
        "case": title,
        "intent": getattr(resp, "intent", None),
        "sentiment": getattr(resp, "sentiment", None),
        "opinion": getattr(resp, "opinion", None),
        "confidence": getattr(resp, "confidence", None),
        "reply": getattr(resp, "reply", None),
        "suggested_tours": [t.title if hasattr(t, 'title') else str(t) for t in (getattr(resp, 'suggested_tours', []) or [])],
        "suggested_questions": [q.question for q in (getattr(resp, 'suggested_questions', []) or [])],
        "tool_calls": [tc.tool_name for tc in (getattr(resp, 'tool_calls', []) or [])]
    }

    print(json.dumps(out, ensure_ascii=False, indent=2))


def main():
    loop = asyncio.get_event_loop()
    tasks = []
    for i, (title, msg) in enumerate(TEST_CASES, start=1):
        tasks.append(run_test_case(i, title, msg))
    loop.run_until_complete(asyncio.gather(*tasks))


if __name__ == "__main__":
    main()
