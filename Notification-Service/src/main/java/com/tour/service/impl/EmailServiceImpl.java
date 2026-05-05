package com.tour.service.impl;

import com.tour.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Chào mừng " + name + " đến với GoTour!");
        message.setText("Tài khoản của bạn đã được khởi tạo thành công trên hệ thống GoTour.");
        mailSender.send(message);
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã OTP xác nhận quên mật khẩu");
        message.setText("Mã xác thực của bạn là: " + otp + ". Hiệu lực trong 5 phút.");
        mailSender.send(message);
    }
}