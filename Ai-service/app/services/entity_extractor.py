"""
Enhanced Entity Extractor with priority-based extraction.
Fixes: BK123 as budget bug, tháng 11 as budget bug, regex conflicts.
"""
import re
from typing import Dict, Optional
from pydantic import BaseModel


class ExtractedEntity(BaseModel):
    """Structured entity extraction result"""
    booking_id: Optional[str] = None
    destination: Optional[str] = None
    month: Optional[int] = None
    budget: Optional[float] = None
    days: Optional[int] = None
    group: Optional[str] = None
    confidence: float = 0.5


# Knowledge base
VIETNAM_DESTINATIONS = [
    "Đà Lạt", "Đà Nẵng", "Hà Nội", "Sài Gòn", "TP.HCM", "HCM",
    "Phú Quốc", "Nha Trang", "Huế", "Hạ Long", "Cát Bà", "Quy Nhơn",
    "Cần Thơ", "Mekong", "Ninh Bình", "Sapa", "Hà Giang", "Đắk Lắk",
    "Da Lat", "Da Nang", "Ha Noi", "Sai Gon"
]

INTERNATIONAL_DESTINATIONS = [
    "Nhật Bản", "Thái Lan", "Campuchia", "Lào", "Myanmar",
    "Singapore", "Malaysia", "Indonesia", "Hàn Quốc", "Trung Quốc",
    "Đài Loan", "Philippines", "Việt Nam",
    "Japan", "Thailand", "Cambodia", "Laos", "South Korea"
]

TRAVEL_GROUPS = [
    "gia đình", "bạn", "người yêu", "cặp đôi", "nhóm",
    "cô đơn", "đôi", "trẻ em", "tuổi vàng", "team",
    "family", "friend", "couple", "solo", "group"
]

# Regex patterns
BOOKING_ID_PATTERN = r"\bBK\d{3,5}\b"
MONTH_PATTERN = r"(?:tháng|month|t)\.?\s*(\d{1,2})\b"
BUDGET_PATTERN = r"(\d+\.?\d*)\s*(?:[MmTt](?:iệu|r|riệu)?|triệu|tr)\b"
DAYS_PATTERN = r"(\d+)\s*(?:ngày|day|n|n2d|3n2d|ngay)\b"


class EntityExtractor:
    """
    Priority-based entity extraction.
    
    Extraction Order (CRITICAL):
    1. BOOKING_ID first (prevents BK123 from being parsed as budget 123M)
    2. MONTH next (prevents "tháng 11" from being parsed as budget 11M)
    3. BUDGET (safe to parse now, BK and MONTH already removed)
    4. DESTINATION (any destination from knowledge base)
    5. DAYS (duration in days)
    6. GROUP (travel group type)
    """
    
    def extract(self, message: str) -> ExtractedEntity:
        """Main extraction method with priority ordering"""
        
        # Normalize: lowercase, but keep some structure
        msg_normalized = message.lower()
        msg_working = msg_normalized  # Will be modified as we extract
        
        result = ExtractedEntity()
        
        # ========== STEP 1: BOOKING_ID (HIGHEST PRIORITY) ==========
        booking_match = re.search(BOOKING_ID_PATTERN, msg_normalized, re.IGNORECASE)
        if booking_match:
            result.booking_id = booking_match.group().upper()
            # Remove from working string to prevent further parsing
            msg_working = msg_working.replace(booking_match.group(), "")
        
        # ========== STEP 2: MONTH (SECOND PRIORITY) ==========
        month_match = re.search(MONTH_PATTERN, msg_normalized)
        if month_match:
            month_val = int(month_match.group(1))
            if 1 <= month_val <= 12:
                result.month = month_val
                # Remove from working string
                msg_working = msg_working.replace(month_match.group(), "")
        
        # ========== STEP 3: BUDGET (NOW SAFE - BK and MONTH removed) ==========
        budget_match = re.search(BUDGET_PATTERN, msg_working)
        if budget_match:
            try:
                budget_val = float(budget_match.group(1))
                # Assume it's in millions (3 triệu = 3,000,000)
                result.budget = budget_val * 1_000_000
            except ValueError:
                pass
        
        # ========== STEP 4: DESTINATION ==========
        # First check exact Vietnam destinations
        for dest in VIETNAM_DESTINATIONS:
            if dest.lower() in msg_normalized:
                result.destination = dest
                break
        
        # If not found, check international
        if not result.destination:
            for dest in INTERNATIONAL_DESTINATIONS:
                if dest.lower() in msg_normalized:
                    result.destination = dest
                    break
        
        # ========== STEP 5: DAYS ==========
        days_match = re.search(DAYS_PATTERN, msg_normalized)
        if days_match:
            try:
                days_val = int(days_match.group(1))
                if 0 < days_val <= 365:  # Sanity check
                    result.days = days_val
            except ValueError:
                pass
        
        # ========== STEP 6: GROUP ==========
        for group in TRAVEL_GROUPS:
            if group in msg_normalized:
                result.group = group
                break
        
        # ========== CALCULATE CONFIDENCE ==========
        # Higher confidence if more fields extracted
        filled_fields = sum([
            result.booking_id is not None,
            result.destination is not None,
            result.month is not None,
            result.budget is not None,
            result.days is not None,
            result.group is not None,
        ])
        result.confidence = min(0.95, 0.4 + (filled_fields * 0.15))
        
        return result
    
    def to_dict(self) -> Dict:
        """Export as dictionary for easier manipulation"""
        extractor = self
        return {
            "booking_id": extractor.booking_id,
            "destination": extractor.destination,
            "month": extractor.month,
            "budget": extractor.budget,
            "days": extractor.days,
            "group": extractor.group,
        }


# ========== UNIT TESTS ==========
if __name__ == "__main__":
    extractor = EntityExtractor()
    
    # Test cases
    test_cases = [
        {
            "msg": "Tour Đà Lạt giá bao nhiêu?",
            "expected": {"destination": "Đà Lạt"}
        },
        {
            "msg": "Tháng 11 nên đi đâu?",
            "expected": {"month": 11, "destination": None}  # CRITICAL: not budget!
        },
        {
            "msg": "BK123 được mấy ngày?",
            "expected": {"booking_id": "BK123"}  # CRITICAL: not budget!
        },
        {
            "msg": "Ngân sách 5 triệu, Nhật Bản, 3 ngày với gia đình",
            "expected": {
                "budget": 5_000_000,
                "destination": "Nhật Bản",
                "days": 3,
                "group": "gia đình"
            }
        },
        {
            "msg": "Tour Đà Lạt 2N3D, 3 triệu, tháng 10",
            "expected": {
                "destination": "Đà Lạt",
                "days": 2,  # 2N3D → 2 days
                "budget": 3_000_000,
                "month": 10
            }
        },
    ]
    
    print("🧪 Entity Extractor Tests\n" + "="*50)
    for i, test in enumerate(test_cases, 1):
        result = extractor.extract(test["msg"])
        print(f"\n{i}. Input: {test['msg']}")
        print(f"   Expected: {test['expected']}")
        print(f"   Got: {result.dict()}")
        print(f"   Confidence: {result.confidence:.2f}")
