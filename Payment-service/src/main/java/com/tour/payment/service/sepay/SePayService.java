package com.tour.payment.service.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SePayService {

    @Value("${sepay.bank-code}")
    private String bankCode;

    @Value("${sepay.account-no}")
    private String accountNo;

    @Value("${sepay.template}")
    private String template;

    public String generateQrUrl(double amount, String orderInfo) {
        long fixedAmount = BigDecimal.valueOf(amount)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        String encodedInfo = URLEncoder.encode(orderInfo, StandardCharsets.UTF_8);
        return String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=%s",
                accountNo, bankCode, fixedAmount, encodedInfo, template);
    }
}
