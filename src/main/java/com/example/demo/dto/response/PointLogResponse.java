package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLogResponse {
    private Long id;
    private Integer pointChanged;
    private String reason;
    private LocalDateTime createdAt;
}

