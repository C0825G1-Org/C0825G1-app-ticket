package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEventRepository extends JpaRepository<Event, Long> {

}
