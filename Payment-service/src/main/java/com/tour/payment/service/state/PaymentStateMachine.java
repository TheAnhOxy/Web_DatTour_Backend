package com.tour.payment.service.state;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class PaymentStateMachine {

    private final Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);

    public PaymentStateMachine() {
        Map<PaymentEvent, PaymentStatus> pendingTransitions = new EnumMap<>(PaymentEvent.class);
        pendingTransitions.put(PaymentEvent.CALLBACK_SUCCESS, PaymentStatus.SUCCESS);
        pendingTransitions.put(PaymentEvent.CALLBACK_FAILED, PaymentStatus.FAILED);
        pendingTransitions.put(PaymentEvent.CANCEL, PaymentStatus.CANCELED);
        pendingTransitions.put(PaymentEvent.TIMEOUT, PaymentStatus.FAILED);
        transitions.put(PaymentStatus.PENDING, pendingTransitions);

        transitions.put(PaymentStatus.SUCCESS, new EnumMap<>(PaymentEvent.class));
        transitions.put(PaymentStatus.FAILED, new EnumMap<>(PaymentEvent.class));
        transitions.put(PaymentStatus.CANCELED, new EnumMap<>(PaymentEvent.class));
    }

    public PaymentStatus transition(PaymentStatus current, PaymentEvent event) {
        Map<PaymentEvent, PaymentStatus> allowed = transitions.get(current);
        if (allowed == null || !allowed.containsKey(event)) {
            throw new IllegalStateException("Invalid payment transition: " + current + " -> " + event);
        }
        return allowed.get(event);
    }
}
