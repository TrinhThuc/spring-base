package com.example.demo.controller;


import com.example.demo.dto.request.AuthRequest;
import com.example.demo.dto.request.LogoutRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    ApiResponse<AuthResponse> login (@RequestBody AuthRequest authRequest){
        var result = authService.authenticate(authRequest);
        return ApiResponse.<AuthResponse>builder()
                .result(result)
        .build();
    }
    @PostMapping("/logout")
    ApiResponse<String> logout(@RequestBody LogoutRequest request)
    {
        String result = authService.logout(request);
        return ApiResponse.<String>builder().result(result).build();
    }
    @PostMapping("/refresh")
    ApiResponse<AuthResponse> logout(@RequestBody RefreshTokenRequest request)
    {
        AuthResponse result = authService.refreshToken(request);
        return ApiResponse.<AuthResponse>builder().result(result).build();
    }
}
