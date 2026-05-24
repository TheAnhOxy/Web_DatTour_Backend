from typing import Optional
from app.models.chat import ChatResponse, TourSummary, SuggestedQuestion


def _format_money(v: float) -> str:
    try:
        return f"{v:,.0f}".replace(",", ".") + "đ"
    except Exception:
        return str(v)


def _emoji_for_sentiment(sentiment: str) -> str:
    if sentiment == "negative":
        return "😔"
    if sentiment == "positive":
        return "😊"
    return ""


def format_chat_response(response: ChatResponse, mode: str = "", tone: str = "") -> ChatResponse:
    """Compress and format ChatResponse into product-friendly UX.

    Rules enforced:
    - Short summary (1-3 sentences)
    - Max 3 bullet options
    - 1 CTA question
    - Add light emoji based on sentiment
    - Trim long dumps
    """

    # Keep original reply but prefer short summary (first paragraph)
    orig = (response.reply or "").strip()
    if not orig:
        orig = "Mình sẵn sàng hỗ trợ bạn tìm tour hoặc kiểm tra booking."

    # Use first paragraph as short summary
    first_para = orig.split("\n\n")[0]
    if len(first_para) > 220:
        first_para = first_para[:217].rstrip() + "..."

    # Low-confidence behavior: if confidence provided and low, ask a clarifying question
    try:
        conf = getattr(response, "confidence", None)
    except Exception:
        conf = None

    if conf is not None and conf < 0.45:
        # Prefer suggested question if available
        if response.suggested_questions and len(response.suggested_questions) > 0:
            q = getattr(response.suggested_questions[0], "question", str(response.suggested_questions[0]))
            response.reply = q
        else:
            response.reply = "Bạn muốn đi trong nước hay nước ngoài vậy?"
        # mark in metadata that this was low-confidence clarification
        if response.metadata is None:
            from app.models.chat import ChatMetadata
            response.metadata = ChatMetadata(processing_time_ms=None, llm_used="low_confidence_clarify", tokens_used=None)
        else:
            try:
                response.metadata.llm_used = "low_confidence_clarify"
            except Exception:
                pass
        return response

    emoji = _emoji_for_sentiment(getattr(response, "sentiment", ""))

    # Complaint responses should feel empathetic and structured, even if the raw reply is robotic.
    if getattr(response, "sentiment", "") == "negative" and getattr(response, "intent", "") == "complaint":
        base = response.reply.strip() if response.reply else "Mình rất tiếc vì trải nghiệm đó đã làm bạn thất vọng 😔"
        if not base.startswith("Mình rất tiếc"):
            response.reply = (
                "Mình rất tiếc vì trải nghiệm đó đã làm bạn thất vọng 😔\n\n"
                "• Mình đã ghi nhận phản hồi của bạn\n"
                "• CSKH sẽ kiểm tra và hỗ trợ sớm nhất\n\n"
                "Bạn có thể cho mình thêm mã booking hoặc ngày đi để mình hỗ trợ nhanh hơn nhé."
            )
            return response

    # Build bullets from suggested_tours (max 3)
    bullets = []
    tours = response.suggested_tours or []
    for t in tours[:3]:
        try:
            title = getattr(t, "title", str(t))
            price = _format_money(getattr(t, "price", 0))
            days = getattr(t, "duration_days", None)
            location = getattr(t, "location", None)
            parts = [title]
            if location:
                parts.insert(0, location)
            if days:
                parts.append(f"{days}N")
            parts.append(price)
            bullets.append(" • " + " — ".join(parts))
        except Exception:
            continue

    # If no bullets, but there is an opinion, show it as short insight
    insight = getattr(response, "opinion", None) or ""

    # CTA: choose first suggested question only (max 1)
    cta = ""
    if response.suggested_questions:
        first_q = getattr(response.suggested_questions[0], "question", str(response.suggested_questions[0])).strip()
        if first_q:
            cta = first_q
        # enforce max 1
        response.suggested_questions = [response.suggested_questions[0]]

    # Compose final reply
    parts = []
    if emoji:
        parts.append(f"{emoji} {first_para}")
    else:
        parts.append(first_para)

    if insight and not bullets:
        # Use insight as second short line
        parts.append(insight)

    if bullets:
        # Limit to 3 bullets and keep compact
        parts.append("\n".join(bullets))

    if cta:
        parts.append(cta)

    final = "\n\n".join(parts).strip()

    # Remove duplicated leading paragraph if appears twice
    if first_para and final.count(first_para) > 1:
        final = final.replace(f"\n\n{first_para}", "")

    # Enforce maximum lengths
    if len(final) > 1000:
        final = final[:997].rstrip() + "..."

    # Mutate response (acceptable since ChatResponse is a Pydantic model)
    response.reply = final

    # Trim suggested_tours to max 3 and suggested_questions to 1 (already enforced)
    if response.suggested_tours and len(response.suggested_tours) > 3:
        response.suggested_tours = response.suggested_tours[:3]

    return response
