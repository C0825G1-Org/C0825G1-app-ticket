package com.codegym.appticket.repository;

import com.codegym.appticket.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEventCategoryRepository extends JpaRepository<EventCategory,Long> {
}
