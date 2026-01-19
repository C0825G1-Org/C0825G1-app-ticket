package com.codegym.appticket.service;

import com.codegym.appticket.entity.QRCode;
import com.codegym.appticket.entity.Ticket;
import java.util.List;

public interface ITicketService {
    List<Ticket> getTicketsByUserId(Long userId);
    Ticket getTicketById(Long id);
    QRCode getQRCodeByTicketId(Long ticketId);
    com.codegym.appticket.entity.User getUserByEmail(String email);
}
