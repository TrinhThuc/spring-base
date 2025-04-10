package com.example.demo.repository;

import com.example.demo.entity.CheckinLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CheckinLogRepository extends JpaRepository<CheckinLog, String> {
    boolean existsByUserIdAndCheckinDatetimeBetween(String userId, LocalDateTime start, LocalDateTime end);
    int countByUserIdAndMonthKey(String userId, String monthKey);

    List<CheckinLog> findByUserIdAndCheckinDatetimeBetween(String userId, LocalDateTime from, LocalDateTime to);

}
