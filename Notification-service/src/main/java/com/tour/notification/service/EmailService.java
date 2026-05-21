package com.tour.notification.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String name);
    void sendRegistrationOtpEmail(String to, String name, String otp);
    void sendOtpEmail(String to, String otp);
    void sendBookingConfirmationEmail(String to, String name, String bookingCode,
                                      String tourTitle, String startDate,
                                      String amount, String gateway);

    void sendBookingOfficeReservationEmail(String to, String name, String bookingCode,
                                           String tourTitle, String startDate, String amount,
                                           String paymentDueAt, String officeAddress,
                                           String officeHours, String officeHotline);

}
