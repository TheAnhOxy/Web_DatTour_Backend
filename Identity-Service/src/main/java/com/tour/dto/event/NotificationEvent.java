package com.tour.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String channel;    // EMAIL
    private String recipient;  // email user
    private String templateCode; // WELCOME_EMAIL, FORGOT_PASSWORD
    private Map<String, Object> param; // { "otp": "123456", "name": "Anh" }
}