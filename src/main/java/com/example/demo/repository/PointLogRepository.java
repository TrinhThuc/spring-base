package com.example.demo.repository;

import com.example.demo.entity.PointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    Page<PointLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

}

