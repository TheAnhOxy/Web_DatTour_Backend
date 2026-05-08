package com.tour.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StripeWebhookRequest {
    private String transactionId;
    private String status;
    private String idempotencyKey;
}
