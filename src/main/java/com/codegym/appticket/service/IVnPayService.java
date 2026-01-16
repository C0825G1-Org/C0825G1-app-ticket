package com.codegym.appticket.service;

import jakarta.servlet.http.HttpServletRequest;

public interface IVnPayService {
    String createPaymentUrl(HttpServletRequest request, Long orderId, long amount);
    int orderReturn(HttpServletRequest request);
}
