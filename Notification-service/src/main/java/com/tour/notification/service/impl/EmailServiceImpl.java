package com.tour.notification.service.impl;

import com.tour.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String APP_NAME = "HTravel";

    private final JavaMailSender mailSender;

    @Override
    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Chào mừng " + name + " đến với " + APP_NAME + "!");
        message.setText(
            "Xin chào " + name + ",\n\n" +
            "Tài khoản của bạn đã được khởi tạo thành công trên hệ thống " + APP_NAME + ".\n\n" +
            "Chúc bạn có những trải nghiệm tuyệt vời!\n" +
            "Đội ngũ " + APP_NAME
        );
        mailSender.send(message);
    }

    @Override
    public void sendRegistrationOtpEmail(String to, String name, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[" + APP_NAME + "] Mã xác thực đăng ký tài khoản");
        message.setText(
            "Xin chào " + name + ",\n\n" +
            "Cảm ơn bạn đã đăng ký tài khoản tại " + APP_NAME + ".\n\n" +
            "Mã OTP xác thực của bạn là:\n\n" +
            "    ► " + otp + " ◄\n\n" +
            "Mã có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
            "Nếu bạn không thực hiện đăng ký, hãy bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ " + APP_NAME
        );
        mailSender.send(message);
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[" + APP_NAME + "] Mã OTP khôi phục mật khẩu");
        message.setText(
            "Mã xác thực khôi phục mật khẩu của bạn là:\n\n" +
            "    ► " + otp + " ◄\n\n" +
            "Mã có hiệu lực trong 5 phút.\n\n" +
            "Đội ngũ " + APP_NAME
        );
        mailSender.send(message);
    }
}
