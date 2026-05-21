package com.tour.payment.service.stripe;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeService {

    /** Stripe yêu cầu tối thiểu ~50 cent USD; VND ~20.000đ an toàn cho môi trường test. */
    public static final long MIN_VND_AMOUNT = 20_000L;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("[Stripe] SDK initialized.");
    }

    /**
     * Tạo Stripe Checkout Session.
     * VND là zero-decimal currency nên unitAmount = số tiền nguyên (không nhân 100).
     *
     * @return Session chứa getId() → transactionId và getUrl() → paymentUrl
     */
    public Session createCheckoutSession(Long bookingId, String bookingCode, BigDecimal amount) {
        long vndAmount = amount.longValue();
        if (vndAmount < MIN_VND_AMOUNT) {
            throw new RuntimeException(
                    "Số tiền tối thiểu cho Stripe là " + MIN_VND_AMOUNT + "đ (hiện tại: " + vndAmount + "đ). "
                            + "Stripe yêu cầu tương đương ít nhất 50 cent USD.");
        }
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl + "?booking_id=" + bookingId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("vnd")
                                                    .setUnitAmount(amount.longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Đặt tour - Mã đơn: " + bookingCode)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("bookingCode", bookingCode)
                    .build();

            Session session = Session.create(params);
            log.info("[Stripe] Tạo Checkout Session thành công: sessionId={}, bookingCode={}",
                    session.getId(), bookingCode);
            return session;

        } catch (StripeException ex) {
            log.error("[Stripe] Lỗi tạo Checkout Session cho booking {}: {}", bookingCode, ex.getMessage());
            throw new RuntimeException("Không thể tạo Stripe Checkout Session: " + ex.getMessage(), ex);
        }
    }

    /**
     * Lấy trạng thái session trực tiếp từ Stripe (dùng ngay sau khi user redirect về success URL).
     */
    public Session retrieveSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            log.info("[Stripe] Retrieve session {} paymentStatus={}", sessionId, session.getPaymentStatus());
            return session;
        } catch (StripeException ex) {
            log.error("[Stripe] Không thể lấy session {}: {}", sessionId, ex.getMessage());
            throw new RuntimeException("Không thể xác nhận phiên Stripe: " + ex.getMessage(), ex);
        }
    }

    /**
     * Xác thực chữ ký webhook từ Stripe và trả về Event.
     * Bắt buộc dùng raw body (bytes/string gốc chưa parse), không dùng sau khi đã đọc bằng Jackson.
     */
    public Event constructWebhookEvent(String rawPayload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(rawPayload, sigHeader, webhookSecret);
            log.info("[Stripe] Webhook xác thực thành công: type={}", event.getType());
            return event;
        } catch (SignatureVerificationException ex) {
            log.warn("[Stripe] Chữ ký webhook không hợp lệ: {}", ex.getMessage());
            throw new RuntimeException("Stripe webhook signature không hợp lệ", ex);
        }
    }
}
