package com.tour.payment.service.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentGatewayStrategyFactory {

    private final List<PaymentGatewayStrategy> strategies;

    public PaymentGatewayStrategy getStrategy(String gateway) {
        if (gateway == null || gateway.trim().isEmpty()) {
            throw new IllegalArgumentException("Gateway is required");
        }

        return strategies.stream()
                .filter(strategy -> strategy.getGateway().equalsIgnoreCase(gateway.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported payment gateway: " + gateway));
    }
}
