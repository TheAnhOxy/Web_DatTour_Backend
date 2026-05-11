package com.tour.payment.service.stripe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class StripeService {

    @Value("${stripe.checkout-base-url}")
    private String checkoutBaseUrl;

    public String generatePaymentUrl(String orderInfo) {
        String encodedInfo = URLEncoder.encode(orderInfo, StandardCharsets.UTF_8);
        return checkoutBaseUrl + encodedInfo;
    }
}
