package com.tour.notification.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String name);
    void sendRegistrationOtpEmail(String to, String name, String otp);
    void sendOtpEmail(String to, String otp);

}
