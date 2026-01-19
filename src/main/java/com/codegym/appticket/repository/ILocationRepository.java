package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ILocationRepository extends JpaRepository<Location, Long> {
    // Optional: Find by details to reuse locations
    Optional<Location> findByProvinceCityAndWardCommuneAndAddressDetail(String provinceCity, String wardCommune, String addressDetail);
}
