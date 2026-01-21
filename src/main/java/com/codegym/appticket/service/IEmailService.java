package com.codegym.appticket.service;

import com.codegym.appticket.entity.Booking;

public interface IEmailService {
    void sendBookingConfirmation(Booking booking);

    void sendInvoiceWithPdf(Booking booking);
}
