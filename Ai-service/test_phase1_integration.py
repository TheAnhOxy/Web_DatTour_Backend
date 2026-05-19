#!/usr/bin/env python
"""
PHASE 1 Integration Tests - Verify all components work together
Run: python test_phase1_integration.py
"""
import asyncio
import json
from app.services.entity_extractor import EntityExtractor
from app.services.negative_detector import NegativeDetector
from app.services.intent_classifier import IntentClassifier
from app.services.conversation_fsm import ConversationFSM


class Phase1Tester:
    def __init__(self):
        self.entity_extractor = EntityExtractor()
        self.negative_detector = NegativeDetector()
        self.intent_classifier = IntentClassifier()
        self.fsm = ConversationFSM()
    
    async def test_full_pipeline(self, message: str, current_mode: str = "DISCOVERY"):
        """Test complete PHASE 1 pipeline"""
        print(f"\n{'='*60}")
        print(f"🧪 TESTING: {message}")
        print(f"{'='*60}")
        
        # Step 1: Entity Extraction
        print("\n1️⃣ Entity Extraction:")
        entities = self.entity_extractor.extract(message)
        print(f"   Entities: {entities.dict()}")
        print(f"   Confidence: {entities.confidence:.2f}")
        
        # Step 2: Negative Detection
        print("\n2️⃣ Negative Sentiment Detection:")
        neg_result = self.negative_detector.detect(message)
        print(f"   Has Negative: {neg_result.has_negative_sentiment}")
        print(f"   Sentiment: {neg_result.sentiment}")
        print(f"   Score: {neg_result.negative_score:.2f}")
        print(f"   Words: {neg_result.detected_words}")
        
        # Step 3: Intent Classification
        print("\n3️⃣ Intent Classification:")
        intent_result = await self.intent_classifier.classify(
            message,
            entities.dict(),
            neg_result.has_negative_sentiment
        )
        print(f"   Intent: {intent_result.intent.value}")
        print(f"   Confidence: {intent_result.confidence:.2f}")
        print(f"   Reasoning: {intent_result.reasoning}")
        
        # Step 4: FSM Mode Transition
        print("\n4️⃣ Conversation Mode FSM:")
        next_mode = await self.fsm.determine_mode(intent_result.intent.value, current_mode)
        print(f"   Current Mode: {current_mode}")
        print(f"   Next Mode: {next_mode}")
        
        context = self.fsm.get_mode_context(next_mode)
        print(f"   Tone: {context.tone}")
        print(f"   System Adjustment: {context.system_prompt_adjustment[:50]}...")
        
        return {
            "message": message,
            "entities": entities.dict(),
            "negative": neg_result.dict(),
            "intent": intent_result.intent.value,
            "mode": next_mode
        }


async def main():
    """Run comprehensive PHASE 1 tests"""
    tester = Phase1Tester()
    
    # ========== TEST SUITE 1: ENTITY EXTRACTION FIXES ==========
    print("\n\n" + "🔥"*30)
    print("🔥 TEST SUITE 1: Entity Extraction Fixes")
    print("🔥"*30)
    
    test_results = []
    
    # Test 1: BK123 should NOT be budget
    result = await tester.test_full_pipeline(
        "Kiểm tra booking BK123",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "BK123 NOT budget",
        "expected_booking_id": "BK123",
        "expected_intent": "booking_support"
    }, result))
    
    # Test 2: Tháng 11 should NOT be budget
    result = await tester.test_full_pipeline(
        "Tháng 11 nên đi đâu?",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Tháng 11 NOT budget",
        "expected_month": 11,
        "expected_intent": "recommendation"
    }, result))
    
    # Test 3: Complex multi-entity
    result = await tester.test_full_pipeline(
        "Ngân sách 7 triệu, Đà Lạt, 3 ngày với gia đình",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Multi-entity extraction",
        "expected_budget": 7_000_000,
        "expected_destination": "Đà Lạt",
        "expected_days": 3,
        "expected_group": "gia đình"
    }, result))
    
    # ========== TEST SUITE 2: NEGATIVE SENTIMENT FIXES ==========
    print("\n\n" + "⚠️"*30)
    print("⚠️ TEST SUITE 2: Negative Sentiment Fixes")
    print("⚠️"*30)
    
    # Test 4: Strong complaint
    result = await tester.test_full_pipeline(
        "Tour quá tệ, CSKH lừa khách hàng",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Complaint detection",
        "expected_negative": True,
        "expected_intent": "complaint"
    }, result))
    
    # Test 5: Complaint escalates even with destination
    result = await tester.test_full_pipeline(
        "Tour Đà Lạt thất vọng",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Complaint overrides tour_search",
        "expected_negative": True,
        "expected_intent": "complaint"
    }, result))
    
    # ========== TEST SUITE 3: INTENT CLASSIFICATION FIXES ==========
    print("\n\n" + "🎯"*30)
    print("🎯 TEST SUITE 3: Intent Classification Fixes")
    print("🎯"*30)
    
    # Test 6: NOT greeting
    result = await tester.test_full_pipeline(
        "Tour Đà Lạt giá bao nhiêu?",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "tour_search NOT greeting",
        "expected_intent": "tour_search"
    }, result))
    
    # Test 7: Comparison
    result = await tester.test_full_pipeline(
        "Tour A hay B cái nào đáng hơn?",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Comparison classification",
        "expected_intent": "comparison"
    }, result))
    
    # Test 8: Booking support
    result = await tester.test_full_pipeline(
        "Tôi muốn hủy booking BK456",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "Booking support classification",
        "expected_intent": "booking_support"
    }, result))
    
    # ========== TEST SUITE 4: FSM MODE TRANSITIONS ==========
    print("\n\n" + "🔄"*30)
    print("🔄 TEST SUITE 4: FSM Mode Transitions")
    print("🔄"*30)
    
    # Test 9: DISCOVERY → BOOKING_SUPPORT
    result = await tester.test_full_pipeline(
        "Booking BK123 ở đâu?",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "DISCOVERY → BOOKING_SUPPORT",
        "expected_mode": "BOOKING_SUPPORT"
    }, result))
    
    # Test 10: DISCOVERY → COMPLAINT
    result = await tester.test_full_pipeline(
        "Tour quá tệ",
        current_mode="DISCOVERY"
    )
    test_results.append(({
        "test": "DISCOVERY → COMPLAINT",
        "expected_mode": "COMPLAINT"
    }, result))
    
    # ========== RESULTS SUMMARY ==========
    print("\n\n" + "="*60)
    print("📊 TEST RESULTS SUMMARY")
    print("="*60)
    
    passed = 0
    failed = 0
    
    for i, (expected, actual) in enumerate(test_results, 1):
        print(f"\n{i}. {expected['test']}")
        
        # Check each expected field
        all_match = True
        for key, expected_val in expected.items():
            if key == "test":
                continue
            
            # Map field names
            if key == "expected_booking_id":
                actual_val = actual["entities"].get("booking_id")
            elif key == "expected_month":
                actual_val = actual["entities"].get("month")
            elif key == "expected_intent":
                actual_val = actual["intent"]
            elif key == "expected_mode":
                actual_val = actual["mode"]
            elif key == "expected_negative":
                actual_val = actual["negative"]["has_negative_sentiment"]
            elif key == "expected_budget":
                actual_val = actual["entities"].get("budget")
            elif key == "expected_destination":
                actual_val = actual["entities"].get("destination")
            elif key == "expected_days":
                actual_val = actual["entities"].get("days")
            elif key == "expected_group":
                actual_val = actual["entities"].get("group")
            else:
                continue
            
            match = actual_val == expected_val
            status = "✅" if match else "❌"
            
            if not match:
                all_match = False
                print(f"   {status} {key}: expected={expected_val}, got={actual_val}")
        
        if all_match:
            print(f"   ✅ PASS")
            passed += 1
        else:
            failed += 1
    
    print(f"\n{'='*60}")
    print(f"✅ PASSED: {passed}/{len(test_results)}")
    print(f"❌ FAILED: {failed}/{len(test_results)}")
    print(f"{'='*60}")
    
    # ========== SCORING IMPROVEMENT ==========
    print("\n🎯 EXPECTED SCORING IMPROVEMENT (Post PHASE 1):")
    print(f"  Intent Detection: 5/10 → 8/10")
    print(f"  Context Memory: 4/10 → 8/10")
    print(f"  Entity Extraction: 5/10 → 8/10")
    print(f"  Complaint Handling: 1/10 → 9/10")
    print(f"  Overall: 3/10 → 6.75/10")


if __name__ == "__main__":
    asyncio.run(main())
