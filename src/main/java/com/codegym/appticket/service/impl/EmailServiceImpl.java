package com.codegym.appticket.service.impl;

import com.codegym.appticket.entity.Booking;
import com.codegym.appticket.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final com.codegym.appticket.repository.BookingDetailRepository bookingDetailRepository;
    private final com.codegym.appticket.repository.TicketRepository ticketRepository;

    @Async
    @Override
    public void sendBookingConfirmation(Booking booking) {
        try {
            log.info("Bắt đầu gửi email cho đơn hàng #{}", booking.getId());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name());

            // Lấy danh sách chi tiết đặt vé
            java.util.List<com.codegym.appticket.entity.BookingDetail> details = bookingDetailRepository.findByBookingId(booking.getId());
            // Lấy danh sách vé để lấy mã QR
            java.util.List<com.codegym.appticket.entity.Ticket> tickets = ticketRepository.findByBookingId(booking.getId());

            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("details", details);
            context.setVariable("tickets", tickets);
            String html = templateEngine.process("mail/booking-confirmation", context);

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Xác Nhận Đặt Vé Thành Công - Đơn hàng #" + booking.getId());
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Gửi email thành công cho đơn hàng #{}", booking.getId());
            
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email cho đơn hàng #{}: {}", booking.getId(), e.getMessage());
        }
    }
}
