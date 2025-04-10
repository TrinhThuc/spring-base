package com.example.demo.dto.response;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckinLogResponse {
    Long id;
    String userId;
    LocalDateTime checkinDatetime;
    Integer pointAwarded;
    Integer totalCheckins;
    String monthKey; // format: yyyy-MM (e.g. 2025-04)
    LocalDateTime createdAt;
}
