-- ==========================================================
-- UPDATE TOUR ID 5
-- ==========================================================

UPDATE tours
SET
    overview = 'Hành trình khám phá Đảo Ngọc Phú Quốc với những bãi biển xanh ngắt, thưởng thức hải sản tươi ngon và trải nghiệm các hoạt động vui chơi giải trí hàng đầu.',

    itinerary = '[
        {
            "time": "Ngày 01",
            "title": "Chào đón Đảo Ngọc - Ngắm hoàng hôn Dinh Cậu",
            "description": "Xe đón quý khách tại sân bay, nhận phòng khách sạn. Chiều tham quan Dinh Cậu, ngắm hoàng hôn và thưởng thức bún quậy đặc sản."
        },
        {
            "time": "Ngày 02",
            "title": "Khám phá Nam Đảo - Câu cá & Lặn ngắm san hô",
            "description": "Tham quan nhà tù Phú Quốc, nhà máy nước mắm. Lên tàu đi câu cá và lặn ngắm san hô tại quần đảo An Thới. Chiều check-in tại Địa Trung Hải."
        },
        {
            "time": "Ngày 03",
            "title": "Tạm biệt Phú Quốc - Mua sắm đặc sản",
            "description": "Tự do tắm biển, mua sắm tại chợ Dương Đông. Xe đưa đoàn ra sân bay, kết thúc hành trình."
        }
    ]'::jsonb,

    inclusions = '[
        "Xe đưa đón đời mới",
        "Khách sạn 4 sao (2 khách/phòng)",
        "Các bữa ăn theo chương trình (3 bữa chính)",
        "Vé tham quan các điểm",
        "Bảo hiểm du lịch tối đa 50tr/vụ"
    ]'::jsonb,

    exclusions = '[
        "Chi phí cá nhân",
        "Đồ uống trong các bữa ăn",
        "Tiền Tip cho HDV và tài xế",
        "Vé máy bay khứ hồi"
    ]'::jsonb,

    policies = '{
        "childPolicy": "Trẻ em dưới 5 tuổi miễn phí. Từ 5-10 tuổi tính 75% giá người lớn. Trên 10 tuổi tính như người lớn.",
        "cancellationPolicy": "Hủy tour trước 7 ngày: Miễn phí. Từ 3-7 ngày: Phí 50%. Dưới 3 ngày: Không hoàn phí.",
        "notes": "Vui lòng mang theo CCCD/Hộ chiếu. Trang phục phù hợp khi đi biển và tham quan đền chùa."
    }'::jsonb,

    rating = 4.8,
    review_count = 150

WHERE id = 5
  AND overview IS NULL;


-- ==========================================================
-- UPDATE OTHER TOURS
-- ==========================================================

UPDATE tours
SET
    overview = 'Chương trình du lịch đặc sắc mang đến cho bạn những trải nghiệm khó quên cùng dịch vụ chất lượng cao.',

    itinerary = '[
        {
            "time": "Ngày 01",
            "title": "Bắt đầu hành trình",
            "description": "Khởi hành tham quan các địa danh nổi tiếng trong chương trình."
        },
        {
            "time": "Ngày 02",
            "title": "Khám phá & Trải nghiệm",
            "description": "Tham gia các hoạt động văn hóa, giải trí địa phương."
        },
        {
            "time": "Ngày 03",
            "title": "Kết thúc tour",
            "description": "Mua sắm quà lưu niệm và khởi hành về lại điểm đón."
        }
    ]'::jsonb,

    inclusions = '[
        "Hướng dẫn viên suốt tuyến",
        "Phương tiện di chuyển",
        "Bảo hiểm du lịch"
    ]'::jsonb,

    exclusions = '[
        "Thuế VAT",
        "Chi phí ngoài chương trình"
    ]'::jsonb,

    policies = '{
        "childPolicy": "Theo quy định tiêu chuẩn của công ty.",
        "cancellationPolicy": "Vui lòng liên hệ để biết chi tiết.",
        "notes": "Tuân thủ hướng dẫn của HDV để đảm bảo an toàn."
    }'::jsonb,

    rating = 4.5,
    review_count = 50

WHERE id != 5
  AND overview IS NULL;