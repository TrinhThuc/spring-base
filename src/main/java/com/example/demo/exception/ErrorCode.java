package com.example.demo.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;



@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_NOT_EXIST(1009, "Refresh token not existed", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_EXPIRED(1010, "Refresh token has expired", HttpStatus.BAD_REQUEST),
    CHECKIN_NOT_IN_ALLOWED_TIME(1011, "Checkin not in allowed time", HttpStatus.BAD_REQUEST),
    ALREADY_CHECKED_IN(1012, "Alredy checked in", HttpStatus.BAD_REQUEST),
    CHECKIN_LIMIT_REACHED(1013, "Checkin limit reached in month", HttpStatus.BAD_REQUEST),
    INVALID_POINT_VALUE(1015, "Invalid point value", HttpStatus.BAD_REQUEST),
    NOT_ENOUGH_POINT(1014, "Not enough point", HttpStatus.BAD_REQUEST),
    SYSTEM_ERROR(2000, "System error", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_REQUEST(2001, "Too many request", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;


}
