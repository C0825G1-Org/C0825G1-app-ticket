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
    public List<QRCode> getQRCodesByTicketIds(List<Long> ticketIds) {
        return qrCodeRepository.findByTicketIdIn(ticketIds);
    }
}
