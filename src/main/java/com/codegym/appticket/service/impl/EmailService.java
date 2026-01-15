package com.codegym.appticket.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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
}
