package com.example.demo.repository;

import com.example.demo.entity.CheckinLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinLogRepository extends JpaRepository<CheckinLog, String> {
    Optional<CheckinLog> findByUserIdAndMonthKey(String userId, String monthKey);


}
