package com.example.demo.controller;

import com.example.demo.dto.request.DeductPointRequest;
import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody UserRequest request) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@RequestBody UserRequest request, @PathVariable String userId) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(request, userId))
                .build();
    }

    @GetMapping
    public ApiResponse<UserResponse> getAllUser(@RequestParam(value = "name", required = false) String name,
                                                Pageable pageable) {
        return ApiResponse.<UserResponse>builder()
                .pageResult(userService.getAllUser(name, pageable))
                .build();
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getById(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getById(userId))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("delete successful")
                .build();
    }

    @PostMapping("/deduct-point")
    public ApiResponse<UserResponse> deductPoint(@RequestBody DeductPointRequest request) throws InterruptedException {
        return ApiResponse.<UserResponse>builder()
                .result(userService.deductPoint(request))
                .build();
    }
}
