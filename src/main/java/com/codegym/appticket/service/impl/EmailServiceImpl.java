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
    @Async
    @Override
    public void sendInvoiceWithPdf(Booking booking) {
        try {
            log.info("Starting invoice generation for booking #{}", booking.getId());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            // Load data
            java.util.List<com.codegym.appticket.entity.BookingDetail> details = bookingDetailRepository.findByBookingId(booking.getId());

            // Calculate Total Price
            long total = 0;
            for (com.codegym.appticket.entity.BookingDetail detail : details) {
                total += detail.getTicketType().getPrice().multiply(new java.math.BigDecimal(detail.getQuantity())).longValue();
            }

            // Generate QR Code with rich data
            String qrContent = String.format("BookingID:%d|User:%s|Amount:%d|Code:%s", 
                    booking.getId(), 
                    booking.getUser().getEmail(), 
                    total, 
                    booking.getTransactionCode() != null ? booking.getTransactionCode() : "N/A");
            String qrCode = generateQrCodeImage(qrContent, 200, 200);

            Context context = new Context();
            context.setVariable("booking", booking);
            context.setVariable("details", details);
            context.setVariable("totalPrice", total);
            context.setVariable("qrCode", qrCode);

            // Additional dynamic data
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            context.setVariable("bookingDate", booking.getBookingTime() != null ? booking.getBookingTime().format(dateFormatter) : java.time.LocalDateTime.now().format(dateFormatter));
            
            // Format currency
            java.text.NumberFormat curFormatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
            context.setVariable("formattedTotal", curFormatter.format(total));
            context.setVariable("amountInWords", "---");


            // Render HTML Invoice
            String htmlInvoice = templateEngine.process("mail/invoice", context);

            // Generate PDF from HTML
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            
            // Register Font
            try {
                 org.springframework.core.io.ClassPathResource fontResource = new org.springframework.core.io.ClassPathResource("fonts/arial.ttf");
                 renderer.getFontResolver().addFont(fontResource.getURL().toExternalForm(), 
                         com.lowagie.text.pdf.BaseFont.IDENTITY_H, 
                         com.lowagie.text.pdf.BaseFont.EMBEDDED);
            } catch (Exception e) {
                log.warn("Could not load custom font, falling back to default. Error: {}", e.getMessage());
            }

            renderer.setDocumentFromString(htmlInvoice);
            renderer.layout();
            renderer.createPDF(outputStream);
            
            byte[] pdfBytes = outputStream.toByteArray();

            // Prepare Email
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Hóa Đơn Diện Tử - Đơn hàng #" + booking.getId());
            helper.setText("Cảm ơn quý khách đã mua vé. Hóa đơn điện tử được đính kèm trong email này.", true);

            // Attach PDF
            helper.addAttachment("Invoice_" + booking.getId() + ".pdf", new org.springframework.core.io.ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Invoice email sent successfully for booking #{}", booking.getId());

        } catch (Exception e) {
            log.error("Failed to generate or send invoice email for booking #{}", booking.getId(), e);
        }
    }

    private String generateQrCodeImage(String text, int width, int height) throws Exception {
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, height);
        java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return java.util.Base64.getEncoder().encodeToString(pngData);
    }
}
