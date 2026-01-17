package com.codegym.appticket.repository;

import com.codegym.appticket.entity.User;
import com.codegym.appticket.entity.UserLockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserLockHistoryRepository extends JpaRepository<UserLockHistory, Long> {
    List<UserLockHistory> findByUserOrderByTimestampDesc(User user);
}
