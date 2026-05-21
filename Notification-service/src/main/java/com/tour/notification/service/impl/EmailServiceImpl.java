package com.tour.notification.service.impl;

import com.tour.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String APP_NAME = "HTravel";
    private static final String BRAND_COLOR = "#0057a8";
    private static final String ACCENT_COLOR = "#f59e0b";

    private final JavaMailSender mailSender;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi gửi email tới " + to + ": " + e.getMessage(), e);
        }
    }

    /** Wrapper HTML chung cho mọi email */
    private String wrapLayout(String bodyContent) {
        return "<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<style>" +
            "  body{margin:0;padding:0;background:#f4f6fb;font-family:'Segoe UI',Arial,sans-serif;}" +
            "  .wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:12px;" +
            "         box-shadow:0 4px 20px rgba(0,0,0,.08);overflow:hidden;}" +
            "  .header{background:" + BRAND_COLOR + ";padding:28px 32px;text-align:center;}" +
            "  .header h1{margin:0;color:#fff;font-size:24px;letter-spacing:1px;}" +
            "  .header p{margin:4px 0 0;color:rgba(255,255,255,.75);font-size:13px;}" +
            "  .body{padding:32px;color:#333;}" +
            "  .footer{background:#f4f6fb;padding:20px 32px;text-align:center;" +
            "           font-size:12px;color:#999;border-top:1px solid #e8eaf0;}" +
            "  .footer a{color:" + BRAND_COLOR + ";text-decoration:none;}" +
            "</style></head><body>" +
            "<div class='wrap'>" +
            "  <div class='header'>" +
            "    <h1>✈ " + APP_NAME + "</h1>" +
            "    <p>Khám phá – Trải nghiệm – Lưu giữ kỷ niệm</p>" +
            "  </div>" +
            "  <div class='body'>" + bodyContent + "</div>" +
            "  <div class='footer'>" +
            "    © 2026 " + APP_NAME + " · <a href='#'>Chính sách bảo mật</a>" +
            "  </div>" +
            "</div></body></html>";
    }

    // ─── Email methods ────────────────────────────────────────────────────────

    @Override
    public void sendWelcomeEmail(String to, String name) {
        String body =
            "<p style='font-size:16px'>Xin chào <strong>" + name + "</strong>,</p>" +
            "<p>Chào mừng bạn đến với <strong>" + APP_NAME + "</strong> — nền tảng đặt tour du lịch uy tín hàng đầu!</p>" +
            "<p>Tài khoản của bạn đã được tạo thành công. Hãy bắt đầu khám phá những hành trình tuyệt vời đang chờ bạn.</p>" +
            "<div style='margin:28px 0;text-align:center'>" +
            "  <a href='http://localhost:3000' style='background:" + BRAND_COLOR + ";color:#fff;" +
            "     padding:12px 32px;border-radius:8px;text-decoration:none;font-weight:600;font-size:15px'>" +
            "     Khám phá tour ngay</a>" +
            "</div>" +
            "<p style='color:#666;font-size:13px'>Chúc bạn có những chuyến đi thật tuyệt vời!<br><strong>Đội ngũ " + APP_NAME + "</strong></p>";

        sendHtml(to, "Chào mừng " + name + " đến với " + APP_NAME + "!", wrapLayout(body));
    }

    @Override
    public void sendRegistrationOtpEmail(String to, String name, String otp) {
        String body =
            "<p style='font-size:16px'>Xin chào <strong>" + name + "</strong>,</p>" +
            "<p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>" + APP_NAME + "</strong>.</p>" +
            "<p>Mã OTP xác thực của bạn là:</p>" +
            "<div style='margin:24px 0;text-align:center'>" +
            "  <span style='display:inline-block;background:#f4f6fb;border:2px dashed " + BRAND_COLOR + ";" +
            "    border-radius:10px;padding:16px 40px;font-size:36px;font-weight:700;" +
            "    letter-spacing:10px;color:" + BRAND_COLOR + "'>" + otp + "</span>" +
            "</div>" +
            "<p style='color:#e53e3e;font-size:13px'>⚠ Mã có hiệu lực trong <strong>10 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
            "<p style='color:#999;font-size:12px'>Nếu bạn không thực hiện đăng ký, hãy bỏ qua email này.</p>";

        sendHtml(to, "[" + APP_NAME + "] Mã xác thực đăng ký tài khoản", wrapLayout(body));
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        String body =
            "<p style='font-size:16px'>Xin chào,</p>" +
            "<p>Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn.</p>" +
            "<p>Mã OTP khôi phục mật khẩu là:</p>" +
            "<div style='margin:24px 0;text-align:center'>" +
            "  <span style='display:inline-block;background:#f4f6fb;border:2px dashed " + BRAND_COLOR + ";" +
            "    border-radius:10px;padding:16px 40px;font-size:36px;font-weight:700;" +
            "    letter-spacing:10px;color:" + BRAND_COLOR + "'>" + otp + "</span>" +
            "</div>" +
            "<p style='color:#e53e3e;font-size:13px'>⚠ Mã có hiệu lực trong <strong>5 phút</strong>.</p>" +
            "<p style='color:#999;font-size:12px'>Nếu bạn không yêu cầu khôi phục mật khẩu, hãy bỏ qua email này.</p>";

        sendHtml(to, "[" + APP_NAME + "] Mã OTP khôi phục mật khẩu", wrapLayout(body));
    }

    @Override
    public void sendBookingConfirmationEmail(String to, String name, String bookingCode,
                                              String tourTitle, String startDate,
                                              String amount, String gateway) {
        String gatewayLabel = "SEPAY".equalsIgnoreCase(gateway) ? "SePay (QR chuyển khoản)"
                            : "STRIPE".equalsIgnoreCase(gateway) ? "Stripe (Thẻ quốc tế)"
                            : "CASH_OFFICE".equalsIgnoreCase(gateway) ? "Tiền mặt tại văn phòng"
                            : gateway;

        String body =
            "<p style='font-size:16px'>Xin chào <strong>" + name + "</strong>,</p>" +
            "<p>Thanh toán của bạn đã được xác nhận thành công. 🎉</p>" +

            // ── Bảng thông tin đơn hàng ──
            "<table style='width:100%;border-collapse:collapse;margin:24px 0;font-size:14px'>" +
            "  <thead>" +
            "    <tr style='background:" + BRAND_COLOR + ";color:#fff'>" +
            "      <th colspan='2' style='padding:12px 16px;text-align:left;border-radius:8px 8px 0 0'>" +
            "        📋 Thông tin đơn đặt tour" +
            "      </th>" +
            "    </tr>" +
            "  </thead>" +
            "  <tbody>" +
            row("🔖 Mã đặt tour",  "<strong style='color:" + BRAND_COLOR + ";font-size:16px'>" + bookingCode + "</strong>", false) +
            row("🗺 Tour",          tourTitle, true) +
            row("📅 Ngày khởi hành", formatDate(startDate), false) +
            row("💳 Phương thức",   gatewayLabel, true) +
            row("💰 Tổng tiền",     "<strong style='color:#16a34a;font-size:16px'>" + amount + "</strong>", false) +
            "  </tbody>" +
            "</table>" +

            // ── Banner chúc mừng ──
            "<div style='background:#f0fdf4;border-left:4px solid #16a34a;border-radius:0 8px 8px 0;" +
            "  padding:14px 18px;margin:20px 0'>" +
            "  <p style='margin:0;color:#15803d;font-size:14px'>" +
            "    ✅ <strong>Đặt tour thành công!</strong> Chúng tôi sẽ liên hệ xác nhận lịch trình trong vòng 24h." +
            "  </p>" +
            "</div>" +

            "<p style='color:#666;font-size:13px'>Cảm ơn bạn đã tin tưởng và lựa chọn <strong>" + APP_NAME + "</strong>. " +
            "Chúc bạn có một chuyến đi thật tuyệt vời!</p>" +
            "<p style='color:#999;font-size:12px'>Nếu có bất kỳ thắc mắc, vui lòng liên hệ chúng tôi qua email hoặc hotline.</p>";

        sendHtml(to,
            "[" + APP_NAME + "] ✅ Xác nhận đặt tour thành công - " + bookingCode,
            wrapLayout(body));
    }

    private String row(String label, String value, boolean shaded) {
        String bg = shaded ? "background:#f8fafc;" : "";
        return "<tr style='" + bg + "'>" +
               "  <td style='padding:10px 16px;color:#666;width:40%;border-bottom:1px solid #e8eaf0'>" + label + "</td>" +
               "  <td style='padding:10px 16px;border-bottom:1px solid #e8eaf0'>" + value + "</td>" +
               "</tr>";
    }

    @Override
    public void sendBookingOfficeReservationEmail(String to, String name, String bookingCode,
                                                  String tourTitle, String startDate, String amount,
                                                  String paymentDueAt, String officeAddress,
                                                  String officeHours, String officeHotline) {
        String body =
            "<p style='font-size:16px'>Xin chào <strong>" + name + "</strong>,</p>" +
            "<p>Bạn đã chọn <strong>thanh toán tiền mặt tại văn phòng " + APP_NAME + "</strong>. " +
            "Vui lòng hoàn tất thanh toán <strong>trong vòng 48 giờ</strong> kể từ lúc đặt tour (hạn cụ thể bên dưới).</p>" +

            "<table style='width:100%;border-collapse:collapse;margin:24px 0;font-size:14px'>" +
            "  <thead>" +
            "    <tr style='background:" + ACCENT_COLOR + ";color:#fff'>" +
            "      <th colspan='2' style='padding:12px 16px;text-align:left'>📋 Thông tin đặt chỗ</th>" +
            "    </tr>" +
            "  </thead>" +
            "  <tbody>" +
            row("🔖 Mã đặt chỗ", "<strong style='font-size:16px;color:" + BRAND_COLOR + "'>" + bookingCode + "</strong>", false) +
            row("🗺 Tour", tourTitle, true) +
            row("📅 Khởi hành", formatDate(startDate), false) +
            row("💰 Số tiền tại quầy", "<strong style='color:#D32F2F'>" + amount + "</strong>", true) +
            row("⏰ Hạn thanh toán", "<strong style='color:#c96200'>" + paymentDueAt + "</strong>", false) +
            "  </tbody>" +
            "</table>" +

            "<div style='background:#FFF8E1;border-left:4px solid " + ACCENT_COLOR + ";padding:14px 18px;margin:20px 0;border-radius:0 8px 8px 0'>" +
            "  <p style='margin:0 0 8px;font-weight:700;color:#7A4E00'>🏢 Hướng dẫn thanh toán tại quầy</p>" +
            "  <p style='margin:0;font-size:13px;color:#5D4037;line-height:1.6'>" +
            "    • Mang <strong>mã đặt chỗ " + bookingCode + "</strong> và CMND/CCCD<br>" +
            "    • Đến: <strong>" + officeAddress + "</strong><br>" +
            "    • Giờ làm việc: " + officeHours + "<br>" +
            "    • Hotline: " + officeHotline +
            "  </p>" +
            "</div>" +

            "<p style='color:#666;font-size:13px'>" +
            "Sau khi nhận tiền, nhân viên sẽ xác nhận và bạn sẽ nhận email <strong>xác nhận thanh toán thành công</strong>. " +
            "Nếu quá hạn trên (48 giờ sau khi đặt), đơn có thể tự động hủy.</p>";

        sendHtml(to,
            "[" + APP_NAME + "] Hướng dẫn thanh toán tại văn phòng - " + bookingCode,
            wrapLayout(body));
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank() || rawDate.equals("null")) return "N/A";
        try {
            // "2026-06-15T08:00:00" → "15/06/2026 08:00"
            String[] parts = rawDate.split("T");
            String[] ymd = parts[0].split("-");
            String time = parts.length > 1 ? " " + parts[1].substring(0, 5) : "";
            return ymd[2] + "/" + ymd[1] + "/" + ymd[0] + time;
        } catch (Exception e) {
            return rawDate;
        }
    }
}
