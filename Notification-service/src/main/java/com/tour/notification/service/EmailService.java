package com.tour.notification.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String name);
    void sendOtpEmail(String to, String otp);

}
