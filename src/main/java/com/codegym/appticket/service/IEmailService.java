package com.codegym.appticket.service;

import com.codegym.appticket.entity.Booking;

public interface IEmailService {
    void sendBookingConfirmation(Booking booking);

    void sendInvoiceWithPdf(Booking booking);

    void sendEventApprovalNotification(com.codegym.appticket.entity.Event event);

    void sendEventRejectionNotification(com.codegym.appticket.entity.Event event, String reason);

    void sendEventCancellationNotification(com.codegym.appticket.entity.Event event, String reason);

    void sendEventRestorationNotification(com.codegym.appticket.entity.Event event);
}
