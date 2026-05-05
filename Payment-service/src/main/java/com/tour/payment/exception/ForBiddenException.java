package com.tour.payment.exception;


public class ForBiddenException extends RuntimeException {
    public ForBiddenException(String message) {
        super(message);
    }
}