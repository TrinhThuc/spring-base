package com.example.demo.dto.response;

import com.example.demo.enumeration.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;
    UserStatus status;
    LocalDateTime joinDate;
    private Long lotusPoint;
    Set<RoleResponse> roles;
}
