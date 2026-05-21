package com.tour.notification.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void listenNotification(String message) {
        log.info("Received notification message: {}", message);
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<>() {});
            String recipient = (String) event.get("recipient");
            String templateCode = (String) event.get("templateCode");

            @SuppressWarnings("unchecked")
            Map<String, Object> param = (Map<String, Object>) event.getOrDefault("param", Map.of());

            log.info("Processing template={} for recipient={}", templateCode, recipient);

            switch (templateCode) {
                case "WELCOME_EMAIL":
                    emailService.sendWelcomeEmail(recipient, (String) param.get("name"));
                    break;
                case "REGISTRATION_OTP":
                    emailService.sendRegistrationOtpEmail(
                        recipient,
                        (String) param.get("name"),
                        (String) param.get("otp")
                    );
                    break;
                case "FORGOT_PASSWORD":
                    emailService.sendOtpEmail(recipient, (String) param.get("otp"));
                    break;
                case "BOOKING_CONFIRMED":
                    emailService.sendBookingConfirmationEmail(
                        recipient,
                        (String) param.get("name"),
                        (String) param.get("bookingCode"),
                        (String) param.get("tourTitle"),
                        (String) param.get("startDate"),
                        (String) param.get("amount"),
                        (String) param.get("gateway")
                    );
                    break;
                case "BOOKING_OFFICE_RESERVATION":
                    emailService.sendBookingOfficeReservationEmail(
                        recipient,
                        (String) param.get("name"),
                        (String) param.get("bookingCode"),
                        (String) param.get("tourTitle"),
                        (String) param.get("startDate"),
                        (String) param.get("amount"),
                        (String) param.get("paymentDueAt"),
                        (String) param.get("officeAddress"),
                        (String) param.get("officeHours"),
                        (String) param.get("officeHotline")
                    );
                    log.info("Sent BOOKING_OFFICE_RESERVATION email to {}", recipient);
                    break;
                default:
                    log.warn("Unknown template code: {}", templateCode);
            }
        } catch (Exception e) {
            log.error("Failed to process notification message: {}", e.getMessage(), e);
        }
    }
}