package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_detail_id")
    private BookingDetail bookingDetail;

    @Column(name = "ticket_code", unique = true)
    private String ticketCode;

    @Column(name = "used")
    private Boolean used = false;

    @Column(name = "check_in_time")
    private java.time.LocalDateTime checkInTime;
}
