package com.example.demo.service;

import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenService {

    RefreshTokenRepository refreshTokenRepository;
    UserRepository userRepository;

    @NonFinal
    @Value("${jwt.refreshExpireTime}")
    private Long expireTime;

    public RefreshToken createToken(String username) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.findByUsername(username).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED)))
                .refreshToken(UUID.randomUUID().toString())
                .expiryTime(new Date(Instant.now().plusSeconds(expireTime).toEpochMilli()))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken createToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .refreshToken(UUID.randomUUID().toString())
                .expiryTime(new Date(Instant.now().plusSeconds(expireTime).toEpochMilli()))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public boolean verifyRefreshToken(String refreshToken) {
        try{
            RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(()-> new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST));
            Date expiryTime = token.getExpiryTime();
            if(expiryTime.before(new Date()))
            {
                throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }
            return true;
        }catch (AppException e)
        {
           return false;
        }
    }



}
