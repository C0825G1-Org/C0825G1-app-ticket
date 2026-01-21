package com.codegym.appticket.repository;

import com.codegym.appticket.entity.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
    Optional<QRCode> findByTicketId(Long ticketId);
    java.util.List<QRCode> findByTicketIdIn(java.util.List<Long> ticketIds);
}
