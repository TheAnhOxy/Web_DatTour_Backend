"""
Negative Word Detector - EARLY PIPELINE DETECTION.
Must run BEFORE intent classification.
Negative sentiment → forces intent=complaint + requires_human_support=true
"""
from typing import Dict, List
from pydantic import BaseModel


class NegativeDetectionResult(BaseModel):
    """Result of negative word detection"""
    has_negative_sentiment: bool
    negative_score: float
    detected_words: List[str]
    sentiment: str  # "negative", "neutral", "positive"


# Negative words with weights
NEGATIVE_WORDS = {
    # Strong complaint indicators (2.0+)
    "tệ": 2.0,
    "thất vọng": 2.0,
    "khó chịu": 2.0,
    "bực": 2.0,
    "tức": 2.0,
    "cáo buộc": 2.5,
    "lừa": 2.5,
    "gian lận": 2.5,
    "hoàn tiền": 2.5,
    "refund": 2.5,
    "complaint": 2.0,
    "phàn nàn": 2.0,
    
    # Service quality issues (1.5-2.0)
    "hỏng": 1.5,
    "chậm": 1.5,
    "bỏ mặc": 1.5,
    "không chịu": 1.5,
    "từ chối": 1.5,
    "thể nào": 1.5,
    
    # Mild dissatisfaction (1.0-1.5)
    "chán": 1.0,
    "tẻ": 1.0,
    "buồn": 1.0,
    "đáng thất vọng": 1.5,
    
    # English variants
    "bad": 2.0,
    "poor": 2.0,
    "terrible": 2.0,
    "horrible": 2.5,
    "disgusting": 2.5,
    "waste": 1.5,
    "scam": 2.5,
    "disappointed": 2.0,
    "angry": 2.0,
    "upset": 1.5,
}

# Positive words (to moderate negative sentiment)
POSITIVE_WORDS = {
    "tuyệt": -1.5,
    "tuyệt vời": -1.5,
    "tốt": -1.0,
    "hay": -0.5,
    "đẹp": -0.5,
    "yêu": -0.5,
    "thích": -0.5,
    "excellent": -1.5,
    "great": -1.0,
    "good": -1.0,
    "amazing": -1.5,
    "love": -1.0,
    "wonderful": -1.5,
}


class NegativeDetector:
    """
    Detect negative sentiment EARLY in the pipeline.
    If negative words detected → forces intent=complaint.
    """
    
    def detect(self, message: str) -> NegativeDetectionResult:
        """
        Scan message for negative words.
        Returns early if threshold met.
        """
        msg_lower = message.lower()
        
        negative_score = 0.0
        detected_words = []
        positive_score = 0.0
        
        # ========== SCAN NEGATIVE WORDS ==========
        for word, weight in NEGATIVE_WORDS.items():
            if word in msg_lower:
                negative_score += weight
                detected_words.append(word)
        
        # ========== SCAN POSITIVE WORDS (to moderate) ==========
        for word, modifier in POSITIVE_WORDS.items():
            if word in msg_lower:
                positive_score += abs(modifier)
        
        # ========== NET SCORE ==========
        net_score = negative_score - positive_score
        
        # ========== SENTIMENT CLASSIFICATION ==========
        if net_score >= 2.0:
            sentiment = "negative"
            has_negative = True
        elif net_score >= 1.0:
            sentiment = "negative"
            has_negative = True
        elif net_score < -1.0:
            sentiment = "positive"
            has_negative = False
        else:
            sentiment = "neutral"
            has_negative = False
        
        return NegativeDetectionResult(
            has_negative_sentiment=has_negative,
            negative_score=max(0, net_score),
            detected_words=detected_words,
            sentiment=sentiment
        )


# ========== TESTS ==========
if __name__ == "__main__":
    detector = NegativeDetector()
    
    test_cases = [
        {
            "msg": "Tour quá tệ",
            "expected_negative": True,
            "expected_words": ["tệ"]
        },
        {
            "msg": "Tôi thất vọng về khách sạn",
            "expected_negative": True,
            "expected_words": ["thất vọng"]
        },
        {
            "msg": "CSKH lừa khách hàng",
            "expected_negative": True,
            "expected_words": ["lừa"]
        },
        {
            "msg": "Tour Đà Lạt giá bao nhiêu?",
            "expected_negative": False,
            "expected_words": []
        },
        {
            "msg": "Tour tuyệt vời nhưng đi với bạn chán",
            "expected_negative": False,  # positive override
            "expected_words": ["chán"]
        },
    ]
    
    print("🧪 Negative Detector Tests\n" + "="*50)
    for i, test in enumerate(test_cases, 1):
        result = detector.detect(test["msg"])
        print(f"\n{i}. Input: {test['msg']}")
        print(f"   Expected Negative: {test['expected_negative']}")
        print(f"   Got: {result.has_negative_sentiment}")
        print(f"   Sentiment: {result.sentiment}")
        print(f"   Score: {result.negative_score:.2f}")
        print(f"   Detected: {result.detected_words}")
        status = "✅" if result.has_negative_sentiment == test['expected_negative'] else "❌"
        print(f"   {status}")
