package com.example.demo.dto.request;


import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRequest {

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;
//    Set<String> roles;
}
