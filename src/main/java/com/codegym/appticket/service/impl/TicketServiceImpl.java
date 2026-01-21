package com.codegym.appticket.service.impl;

import com.codegym.appticket.entity.QRCode;
import com.codegym.appticket.entity.Ticket;
import com.codegym.appticket.repository.QRCodeRepository;
import com.codegym.appticket.repository.TicketRepository;
import com.codegym.appticket.service.ITicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements ITicketService {

    private final com.codegym.appticket.repository.IEventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final QRCodeRepository qrCodeRepository;
    private final com.codegym.appticket.repository.UserRepository userRepository;

    @Override
    public List<Ticket> getTicketsByUserId(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    @Override
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Override
    public QRCode getQRCodeByTicketId(Long ticketId) {
        return qrCodeRepository.findByTicketId(ticketId).orElse(null);
    }

    @Override
    public com.codegym.appticket.entity.User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Override
    public List<Ticket> getTicketsByUserIdAndEventId(Long userId, Long eventId) {
        return ticketRepository.findByUserIdAndEventId(userId, eventId);
    }

    @Override
    public List<Ticket> getTicketsByUserIdAndOccurrenceId(Long userId, Long occurrenceId) {
        return ticketRepository.findByUserIdAndOccurrenceId(userId, occurrenceId);
    }

    @Override
    public List<QRCode> getQRCodesByTicketIds(List<Long> ticketIds) {
        return qrCodeRepository.findByTicketIdIn(ticketIds);
    }

    @Override
    public com.codegym.appticket.dto.ticket.TicketCheckInResponse checkInTicket(com.codegym.appticket.dto.ticket.TicketCheckInRequest request) {
        String code = request.getTicketCode() != null ? request.getTicketCode().trim() : "";
        if (code.startsWith("TICKET-")) {
            code = code.substring(7);
        }
        System.out.println("Processing Check-in for Code: [" + code + "] - EventId: " + request.getEventId());

        Ticket ticket = ticketRepository.findByTicketCode(code).orElse(null);

        if (ticket == null) {
             System.out.println("Ticket NOT FOUND for code: " + code);
             // Try debugging: list all codes? No, too many.
             return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                    .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.NOT_FOUND)
                    .message("Mã vé không tồn tại trong hệ thống")
                    .ticketCode(code)
                    .build();
        }
        System.out.println("Ticket FOUND: ID=" + ticket.getId() + ", Code=" + ticket.getTicketCode());

        // Validate Event
        Long ticketEventId = ticket.getBookingDetail().getTicketType().getEventOccurrence().getEvent().getId();
        if (!ticketEventId.equals(request.getEventId())) {
            return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                    .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.INVALID_EVENT)
                    .message("Vé không thuộc sự kiện này")
                    .ticketCode(request.getTicketCode())
                    .build();
        }

        // Validate Duplicate
        if (ticket.getUsed()) {
            return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                    .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.ALREADY_CHECKED_IN)
                    .message("Vé đã được sử dụng vào lúc: " + ticket.getCheckInTime())
                    .ticketCode(request.getTicketCode())
                    .customerName(ticket.getBookingDetail().getBooking().getUser().getFullName())
                    .ticketType(ticket.getBookingDetail().getTicketType().getName())
                    .checkInTime(ticket.getCheckInTime())
                    .build();
        }

        // Validate Time Window (3 hours before start time)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startTime = ticket.getBookingDetail().getTicketType().getEventOccurrence().getStartTime();
        java.time.LocalDateTime endTime = ticket.getBookingDetail().getTicketType().getEventOccurrence().getEndTime();

        // Allow check-in 3 hours before start
        java.time.LocalDateTime validCheckInStart = startTime.minusHours(3);

        if (now.isBefore(validCheckInStart)) {
             return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                    .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.INVALID_TIME)
                    .message("Sự kiện chưa diễn ra. Vui lòng quay lại sau " + validCheckInStart)
                    .ticketCode(request.getTicketCode())
                    .build();
        }
        
         // Optional: Check if event ended (e.g. + 1 hour buffer)
         if (now.isAfter(endTime.plusHours(1))) { // 1 hour buffer after end
             return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                    .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.INVALID_TIME)
                    .message("Sự kiện đã kết thúc")
                    .ticketCode(request.getTicketCode())
                    .build();
         }


        // Success
        ticket.setUsed(true);
        ticket.setCheckInTime(now);
        ticketRepository.save(ticket);

        return com.codegym.appticket.dto.ticket.TicketCheckInResponse.builder()
                .status(com.codegym.appticket.dto.ticket.TicketCheckInResponse.Status.SUCCESS)
                .message("Check-in thành công")
                .ticketCode(ticket.getTicketCode())
                .customerName(ticket.getBookingDetail().getBooking().getUser().getFullName())
                .ticketType(ticket.getBookingDetail().getTicketType().getName())
                .checkInTime(now)
                .build();
    }

    @Override
    public java.util.Map<String, Object> getCheckInStats(Long eventId) {
        Long totalTickets = eventRepository.findBookedTicketsByEventAndOccurrence(eventId, null).stream()
                .mapToLong(dto -> dto.getQuantity().longValue()).sum(); // Actually bookedTickets returns quantity per row (which is aggregation of TicketType?). 
                // Wait, BookedTicketDTO.quantity is count.
                // Let's re-check BookedTicketDTO logic. The query computes row per bookingDetail?
                // Query: SELECT ... bd.quantity ... FROM Booking b JOIN b.bookingDetails bd ...
                // Yes, bd.quantity is number of tickets.
        
        // Wait, efficient way:
        // Total Tickets: sum of quantity in bookingDetails for this event.
        // Checked In: countCheckedInTickets query in IEventRepository.
        
        // Using existing method for total might be heavy if list is huge.
        // But for now it's okay.
        
        long totalSold = eventRepository.findBookedTicketsByEventAndOccurrence(eventId, null).stream()
                .mapToLong(t -> t.getQuantity()).sum();
        
        Long checkedIn = eventRepository.countCheckedInTickets(eventId);
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalTickets", totalSold);
        stats.put("checkedIn", checkedIn != null ? checkedIn : 0);
        stats.put("invalid", 0); // Placeholder, maybe log invalid attempts in future?
        
        return stats;
    }

    @Override
    public List<com.codegym.appticket.dto.ticket.CheckInHistoryDTO> getCheckInHistory(Long eventId) {
        return ticketRepository.findCheckedInTicketsByEventId(eventId);
    }
}
