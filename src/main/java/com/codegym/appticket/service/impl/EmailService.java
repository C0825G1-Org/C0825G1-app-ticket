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

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("triphung15@gmail.com", "AppTicket Support");
        helper.setTo(toEmail);

        String subject = "Mã xác thực đăng ký tài khoản - AppTicket";

        String content = "<p>Xin chào,</p>"
                + "<p>Bạn đã đăng ký tài khoản trên AppTicket.</p>"
                + "<p>Mã xác thực (OTP) của bạn là:</p>"
                + "<h2>" + otpCode + "</h2>"
                + "<p>Mã này sẽ hết hạn sau 5 phút.</p>"
                + "<p>Vui lòng nhập mã này để kích hoạt tài khoản.</p>"
                + "<br>"
                + "<p>Trân trọng,</p>"
                + "<p>AppTicket Team</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    @Async
    public void sendAccountCreatedEmail(String toEmail, String fullName, String password) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("triphung15@gmail.com", "AppTicket Support");
        helper.setTo(toEmail);

        String subject = "Thông tin tài khoản AppTicket của bạn";

        String content = "<p>Xin chào <b>" + fullName + "</b>,</p>"
                + "<p>Tài khoản của bạn đã được tạo thành công trên hệ thống AppTicket bởi quản trị viên.</p>"
                + "<p>Thông tin đăng nhập:</p>"
                + "<ul>"
                + "<li><b>Email:</b> " + toEmail + "</li>"
                + "<li><b>Mật khẩu:</b> " + password + "</li>"
                + "</ul>"
                + "<p style='color: red; font-weight: bold;'>Lưu ý quan trọng: Vì lý do bảo mật, vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu tiên.</p>"
                + "<p>Truy cập hệ thống tại: <a href='http://localhost:8080/login'>Đăng nhập AppTicket</a></p>"
                + "<br>"
                + "<p>Trân trọng,</p>"
                + "<p>AppTicket Team</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    @Async
    public void sendLockNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản bị khóa", 
            "Tài khoản AppTicket của bạn đã bị KHÓA.", reason);
    }

    @Async
    public void sendUnlockNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản được mở khóa", 
            "Tài khoản AppTicket của bạn đã được MỞ KHÓA.", reason);
    }

    @Async
    public void sendDeleteNotification(String toEmail, String fullName, String reason) throws MessagingException, UnsupportedEncodingException {
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản bị xóa", 
            "Tài khoản AppTicket của bạn đã bị XÓA.", reason);
    }

    @Async
    public void sendAutoDeleteNotification(String toEmail, String fullName) throws MessagingException, UnsupportedEncodingException {
        String reason = "Bạn đã không phản hồi lại email thông báo khóa tài khoản trước đó, hoặc phản hồi không hợp lệ.";
        sendAdminActionEmail(toEmail, fullName, "Thông báo: Tài khoản tự động bị xóa", 
            "Tài khoản AppTicket của bạn đã bị XÓA TỰ ĐỘNG khỏi hệ thống.", reason);
    }

    private void sendAdminActionEmail(String toEmail, String fullName, String subject, String actionTitle, String reason) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("rockmanx97dn@gmail.com", "AppTicket Admin");
        helper.setTo(toEmail);

        String content = "<p>Xin chào <b>" + fullName + "</b>,</p>"
                + "<p>" + actionTitle + "</p>"
                + "<p><b>Lý do:</b> " + reason + "</p>"
                + "<p>Vui lòng phản hồi email này trong vòng 30 ngày tính từ lúc nhận email nếu bạn có thắc mắc hoặc cần xác nhận. Sau khoảng thời gian này nếu không reply xác nhận hoặc reply không được chấp thuận thì tài khoản của bạn sẽ bị xóa khỏi hệ thống.</p>"
                + "<br>"
                + "<p>Trân trọng,</p>"
                + "<p>Ban quản trị AppTicket</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }
}
