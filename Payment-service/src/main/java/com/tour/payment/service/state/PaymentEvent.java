package com.tour.payment.service.state;

public enum PaymentEvent {
    CALLBACK_SUCCESS,
    CALLBACK_FAILED,
    CANCEL,
    TIMEOUT
}
