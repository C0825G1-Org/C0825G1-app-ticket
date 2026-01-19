package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ILocationRepository extends JpaRepository<Location, Long> {
    // Optional: Find by details to reuse locations
    @org.springframework.data.jpa.repository.Query("SELECT l FROM Location l WHERE l.ward.code = :wardCode AND l.addressDetail = :addressDetail")
    Optional<Location> findByWardCodeAndAddressDetail(@org.springframework.data.repository.query.Param("wardCode") Integer wardCode, @org.springframework.data.repository.query.Param("addressDetail") String addressDetail);
}
