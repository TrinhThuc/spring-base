package com.example.demo.service;

import com.example.demo.dto.response.CheckinLogResponse;
import com.example.demo.dto.response.CheckinStatusResponse;
import com.example.demo.dto.response.PointLogResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AttendanceService {

    CheckinLogResponse checkin();

    List<CheckinStatusResponse> getCheckinStatuses(int year, int month);

    Page<PointLogResponse> getPointLogs(int page, int size);
}
