package com.tour.payment.service.callback;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCallbackData {
    private final String transactionId;
    private final String status;
    private final String idempotencyKey;
    private final java.math.BigDecimal amount;
}
