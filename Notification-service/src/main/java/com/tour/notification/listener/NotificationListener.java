package com.tour.notification.listener;

import com.tour.notification.dto.event.NotificationEvent;
import com.tour.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void listenNotification(NotificationEvent event) {
        log.info("Received notification event for: {}", event.getRecipient());

        try {
            switch (event.getTemplateCode()) {
                case "WELCOME_EMAIL":
                    emailService.sendWelcomeEmail(event.getRecipient(), (String) event.getParam().get("name"));
                    break;
                case "FORGOT_PASSWORD":
                    emailService.sendOtpEmail(event.getRecipient(), (String) event.getParam().get("otp"));
                    break;
                default:
                    log.warn("Unknown template code: {}", event.getTemplateCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.getRecipient(), e.getMessage());
        }
    }
}