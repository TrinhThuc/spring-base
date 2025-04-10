package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckinStatusResponse {
    private LocalDate date;
    private boolean checkedIn;
    private LocalDateTime checkinTime;
    private Integer pointAwarded;
}

