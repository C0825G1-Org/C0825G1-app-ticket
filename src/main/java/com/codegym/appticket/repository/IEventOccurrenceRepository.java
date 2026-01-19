package com.codegym.appticket.repository;

import com.codegym.appticket.entity.EventOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IEventOccurrenceRepository extends JpaRepository<EventOccurrence, Long> {

    @Query("SELECT eo FROM EventOccurrence eo " +
           "WHERE eo.location.id = :locationId " +
           "AND (:startTime < eo.endTime AND :endTime > eo.startTime)")
    List<EventOccurrence> findConflicts(
            @Param("locationId") Long locationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
