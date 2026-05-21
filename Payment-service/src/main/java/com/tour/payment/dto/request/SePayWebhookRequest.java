package com.tour.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload thực tế SePay gửi về webhook endpoint.
 * Tham khảo: https://sepay.vn/tai-lieu-tich-hop.html
 *
 * Ví dụ body:
 * {
 *   "id": 12345,
 *   "gateway": "ICB",
 *   "transactionDate": "2024-01-01 12:00:00",
 *   "accountNumber": "109876820087",
 *   "content": "SEVQRBK394662931746900",
 *   "transferType": "in",
 *   "transferAmount": 2900000,
 *   "referenceCode": "FT24001234567890",
 *   "code": "SEVQRBK394662931746900"
 * }
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayWebhookRequest {

    /** ID giao dịch nội bộ của SePay */
    private Long id;

    /** Nội dung chuyển khoản — chứa mã đặt chỗ, vd: "SEVQRBK394662931746900" */
    private String content;

    /** Số tiền giao dịch */
    private Double transferAmount;

    /** Loại giao dịch: "in" = nhận tiền */
    private String transferType;

    /** Mã tham chiếu ngân hàng */
    private String referenceCode;

    /** Số tài khoản nhận */
    private String accountNumber;

    /** Ngân hàng */
    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    // ── Các field dùng cho luồng test thủ công (giữ lại để không break) ──

    /** Dùng khi test thủ công (POST trực tiếp không qua SePay) */
    private String transactionId;

    /** Trạng thái — dùng khi test thủ công */
    private String status;

    /** Idempotency key — dùng khi test thủ công */
    private String idempotencyKey;

    /**
     * Trả về transactionId để dùng trong processCallback.
     *
     * SePay gửi content dạng: "130162279595-0786012569-SEVQRBK7214394207400"
     * → cần extract phần bắt đầu từ "SEVQR" trở đi: "SEVQRBK7214394207400"
     * Phần này khớp với transactionId đã lưu trong DB.
     *
     * Fallback về field "transactionId" khi test thủ công.
     */
    public String resolveTransactionId() {
        if (content != null && !content.isBlank()) {
            int idx = content.indexOf("SEVQR");
            if (idx >= 0) return content.substring(idx).trim();
            return content.trim(); // không có SEVQR prefix → dùng nguyên
        }
        return transactionId;
    }

    /**
     * Trả về status để dùng trong processCallback:
     * - Webhook thật từ SePay chỉ gửi khi giao dịch thành công và transferType="in"
     *   → luôn là SUCCESS
     * - Fallback về field "status" khi test thủ công
     */
    public String resolveStatus() {
        if (content != null && !content.isBlank()) {
            return "in".equalsIgnoreCase(transferType) ? "SUCCESS" : "FAILED";
        }
        return status != null ? status : "SUCCESS";
    }
}
