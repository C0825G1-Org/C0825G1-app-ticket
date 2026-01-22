package com.codegym.appticket.repository;

import com.codegym.appticket.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IWardRepository extends JpaRepository<Ward, Integer> {
}
