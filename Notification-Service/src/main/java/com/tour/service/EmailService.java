package com.tour.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String name);
    void sendOtpEmail(String to, String otp);

}
