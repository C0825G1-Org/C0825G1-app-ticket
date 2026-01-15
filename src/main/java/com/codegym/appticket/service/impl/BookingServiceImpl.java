package com.codegym.appticket.service.impl;

import com.codegym.appticket.entity.*;
import com.codegym.appticket.repository.*;
import com.codegym.appticket.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements IBookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final com.codegym.appticket.repository.QRCodeRepository qrCodeRepository;
    private final com.codegym.appticket.service.IEmailService emailService;

    @Override
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
    }

    @Override
    public List<TicketType> getTicketTypesByEventId(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId);
    }

    @Override
    @Transactional
    public Booking createBooking(Long eventId, Long userId, Map<Long, Integer> ticketQuantities) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.SUCCESS); // Mặc định là thành công cho website truyền thống này
        booking = bookingRepository.save(booking);

        for (Map.Entry<Long, Integer> entry : ticketQuantities.entrySet()) {
            Long ticketTypeId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity <= 0) continue;

            TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                    .orElseThrow(() -> new RuntimeException("Ticket type not found"));

            if (ticketType.getQuantity() < quantity) {
                throw new RuntimeException("Not enough tickets for: " + ticketType.getName());
            }

            // Giảm số lượng vé trong kho
            ticketType.setQuantity(ticketType.getQuantity() - quantity);
            ticketTypeRepository.save(ticketType);

            // Tạo chi tiết booking
            BookingDetail detail = new BookingDetail();
            detail.setBooking(booking);
            detail.setTicketType(ticketType);
            detail.setQuantity(quantity);
            detail = bookingDetailRepository.save(detail);

            // Tạo các vé (Ticket) và dữ liệu QR tương ứng
            for (int i = 0; i < quantity; i++) {
                Ticket ticket = new Ticket();
                ticket.setBookingDetail(detail);
                String ticketCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                ticket.setTicketCode(ticketCode);
                ticket.setUsed(false);
                ticket = ticketRepository.save(ticket);

                // Tác vụ bổ sung: Sinh dữ liệu mã QR
                com.codegym.appticket.entity.QRCode qr = new com.codegym.appticket.entity.QRCode();
                qr.setTicket(ticket);
                qr.setQrData("TICKET-" + ticketCode);
                qrCodeRepository.save(qr);
            }
        }

        // Tác vụ bổ sung: Gửi Amazon/Gmail xác nhận
        // Chúng ta lấy bản ghi booking đầy đủ để đảm bảo có List<BookingDetail>
        Booking finalBooking = bookingRepository.findById(booking.getId()).orElse(booking);
        emailService.sendBookingConfirmation(finalBooking);

        return booking;
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Hoàn lại số lượng vé
        List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
        for (BookingDetail detail : details) {
            TicketType tt = detail.getTicketType();
            tt.setQuantity(tt.getQuantity() + detail.getQuantity());
            ticketTypeRepository.save(tt);
        }
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public List<Ticket> getTicketsByBookingId(Long bookingId) {
        return ticketRepository.findByBookingId(bookingId);
    }
}
