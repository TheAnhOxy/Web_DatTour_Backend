package com.tour.payment.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
