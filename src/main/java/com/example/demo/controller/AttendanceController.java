package com.example.demo.controller;

import com.example.demo.common.Constants;
import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.*;
import com.example.demo.entity.User;
import com.example.demo.service.AttendanceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ApiResponse<CheckinLogResponse> Checkin() {

        return ApiResponse.<CheckinLogResponse>builder()
                .result(attendanceService.checkin())
                .build();
    }

    @GetMapping("/checkin/status")
    public ResponseEntity<List<CheckinStatusResponse>> getCheckinStatuses() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        List<CheckinStatusResponse> statuses = attendanceService.getCheckinStatuses(year, month);
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/point-logs")
    public ResponseEntity<Page<PointLogResponse>> getPointLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PointLogResponse> logs = attendanceService.getPointLogs(page, size);
        return ResponseEntity.ok(logs);
    }

}
