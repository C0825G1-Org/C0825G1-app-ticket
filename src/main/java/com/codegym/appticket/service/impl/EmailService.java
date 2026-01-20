package com.codegym.appticket.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

import org.springframework.scheduling.annotation.Async;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private String getEmailTemplate(String title, String content) {
        return "<!DOCTYPE html>"
             + "<html>"
             + "<head>"
             + "<meta charset='UTF-8'>"
             + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
             + "</head>"
             + "<body style='margin: 0; padding: 0; background-color: #f8f9fa; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif;'>"
             + "    <table role='presentation' border='0' cellpadding='0' cellspacing='0' width='100%'>"
             + "        <tr>"
             + "            <td style='padding: 40px 20px;'>"
             + "                <table role='presentation' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden;'>"
             + "                    <tr>"
             + "                        <td style='padding: 40px;'>"
             + "                            <h2 style='margin-top: 0; margin-bottom: 20px; color: #333333; font-size: 20px;'>" + title + "</h2>"
             +                              content
             + "                        </td>"
             + "                    </tr>"
             + "                </table>"
             + "            </td>"
             + "        </tr>"
             + "        <tr>"
             + "            <td style='padding: 20px; text-align: center; color: #6c757d; font-size: 14px;'>"
             + "                <p style='margin: 0;'>&copy; 2026 AppTicket. All rights reserved.</p>"
             + "            </td>"
             + "        </tr>"
             + "    </table>"
             + "</body>"
             + "</html>";
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("triphung15@gmail.com", "AppTicket Support");
        helper.setTo(toEmail);

        String subject = "Mã xác thực đăng ký tài khoản - AppTicket";
        String body = "<p style='color: #555; margin-bottom: 20px;'>Xin chào,</p>"
                    + "<p style='color: #555; margin-bottom: 20px;'>Bạn đã yêu cầu đăng ký tài khoản hoặc đặt lại mật khẩu trên AppTicket. Mã xác thực (OTP) của bạn là:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "    <span style='background-color: #e9ecef; padding: 15px 30px; border-radius: 8px; font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #0d6efd;'>" + otpCode + "</span>"
                    + "</div>"
                    + "<p style='color: #555; margin-bottom: 10px;'>Mã này sẽ hết hạn sau <strong>5 phút</strong>.</p>"
                    + "<p style='color: #555;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>";

        helper.setSubject(subject);
        helper.setText(getEmailTemplate("Xác thực tài khoản", body), true);
        mailSender.send(message);
    }

    @Async
    public void sendAccountCreatedEmail(String toEmail, String fullName, String password) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("triphung15@gmail.com", "AppTicket Support");
        helper.setTo(toEmail);

        String subject = "Chào mừng bạn đến với AppTicket";
        String body = "<p style='color: #555; margin-bottom: 20px;'>Xin chào <b>" + fullName + "</b>,</p>"
                    + "<p style='color: #555; margin-bottom: 20px;'>Tài khoản của bạn đã được tạo thành công bởi quản trị viên.</p>"
                    + "<div style='background-color: #f8f9fa; border-left: 4px solid #0d6efd; padding: 20px; border-radius: 4px; margin-bottom: 25px;'>"
                    + "    <p style='margin: 0 0 10px;'><strong>Email:</strong> " + toEmail + "</p>"
                    + "    <p style='margin: 0;'><strong>Mật khẩu:</strong> " + password + "</p>"
                    + "</div>"
                    + "<p style='color: #dc3545; font-weight: bold; margin-bottom: 25px;'><i style='font-style: normal;'>⚠️</i> Vì lý do bảo mật, vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu tiên.</p>"
                    + "<div style='text-align: center; margin-bottom: 20px;'>"
                    + "    <a href='http://localhost:8080/login' style='background-color: #0d6efd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Đăng nhập ngay</a>"
                    + "</div>";

        helper.setSubject(subject);
        helper.setText(getEmailTemplate("Tài khoản được tạo thành công", body), true);
        mailSender.send(message);
    }

    @Async
    public void sendLockNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản bị khóa", 
            "Tài khoản của bạn đã bị KHÓA", reason, "#ffc107", "Tạm khóa", true); // Include warning
    }

    @Async
    public void sendUnlockNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản được mở khóa", 
            "Tài khoản của bạn đã được MỞ KHÓA", reason, "#198754", "Mở khóa", false); // No warning
    }

    @Async
    public void sendDeleteNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản bị xóa", 
            "Tài khoản của bạn đã bị XÓA", reason, "#dc3545", "Xóa vĩnh viễn", false); // No warning
    }

    @Async
    public void sendAutoDeleteNotification(String toEmail, String fullName) throws MessagingException, UnsupportedEncodingException {
        String reason = "Bạn đã không phản hồi lại email thông báo khóa tài khoản trước đó, hoặc phản hồi không hợp lệ trong thời gian quy định.";
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản tự động bị xóa", 
            "Tài khoản bị XÓA TỰ ĐỘNG", reason, "#dc3545", "Xóa tự động", false); // No warning
    }

    private void sendAdminActionEmail(String toEmail, String fullName, String subject, String title, String reason, String colorCode, String badgeText, boolean includeWarning) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("triphung15@gmail.com", "AppTicket Admin");
        helper.setTo(toEmail);

        String body = "<p style='color: #555; margin-bottom: 20px;'>Xin chào <b>" + fullName + "</b>,</p>"
                    + "<div style='text-align: center; margin-bottom: 25px;'>"
                    + "    <span style='background-color: " + colorCode + "; color: white; padding: 8px 16px; border-radius: 50px; font-weight: bold; font-size: 14px; text-transform: uppercase;'>" + badgeText + "</span>"
                    + "</div>"
                    + "<h3 style='text-align: center; color: #333; margin-bottom: 20px;'>" + title + "</h3>"
                    + "<div style='background-color: #fff3cd; border: 1px solid #ffecb5; color: #664d03; padding: 20px; border-radius: 8px; margin-bottom: 25px;'>"
                    + "    <p style='margin: 0; font-weight: bold;'>Lý do:</p>"
                    + "    <p style='margin: 5px 0 0;'>" + reason + "</p>"
                    + "</div>";

        if (includeWarning) {
            body += "<p style='color: #555; font-size: 14px; line-height: 1.6;'>Vui lòng phản hồi email này trong vòng <strong>30 ngày</strong> nếu bạn có thắc mắc. Sau thời gian này, tài khoản của bạn sẽ bị xóa vĩnh viễn.</p>";
        }

        helper.setSubject(subject);
        helper.setText(getEmailTemplate(subject, body), true);
        mailSender.send(message);
    }
}
