package com.example.demo.service;


import com.example.demo.common.Constants;
import com.example.demo.common.RedisTTL;
import com.example.demo.dto.request.AuthRequest;
import com.example.demo.dto.request.LogoutRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.InvalidatedToken;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.InvalidatedTokenRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AuthService {

    final UserRepository userRepository;
    final InvalidatedTokenRepository invalidatedTokenRepository;
    final RefreshTokenService refreshTokenService;
    final RefreshTokenRepository refreshTokenRepository;
    final RedisService redisService;


    @NonFinal
    @Value("${jwt.signerKey}")
    private String signerKey;

    @NonFinal
    @Value("${jwt.accessExpireTime}")
    private Long expireTime;

    public AuthResponse authenticate (AuthRequest authRequest){

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticate = passwordEncoder.matches(authRequest.getPassword(), user.getPassword());
        if (!authenticate)
        {
            throw  new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String token = generateToken(user);
        String refreshToken = refreshTokenService.createToken(user).getRefreshToken();
        redisService.setValue(Constants.USER_INFO + user.getUsername(), user, RedisTTL.USER_INFO_TTL, TimeUnit.HOURS);
        return  AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .authenticate(true)
            .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request)
    {
        if(!refreshTokenService.verifyRefreshToken(request.getRefreshToken()))
        {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).orElseThrow(
                ()-> new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST)
        );
        refreshTokenRepository.delete(refreshToken);
        User user = refreshToken.getUser();
        String accessToken = generateToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenService.createToken(user.getUsername()).getRefreshToken())
                .authenticate(true)
                .build();
    }

    public String logout(LogoutRequest request)
    {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());

            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            invalidatedTokenRepository.save(InvalidatedToken.builder()
                            .id(jit)
                            .expiryDate(expiryTime)
                    .build());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return "Logout success";
    };

    public String generateToken(User user){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("doman.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(expireTime, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user) )
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }


    public boolean verifyToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime.before(new Date())) {
                return false;
            }
            if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
                return false;
            }
            return signedJWT.verify(jwsVerifier);
        } catch (ParseException | JOSEException | AppException e) {
            log.error("Cannot verify token", e);
            return false;
        }
    }

    public String buildScope(User user)
    {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if(!user.getRoles().isEmpty()) {
            user.getRoles().forEach(role ->
                    stringJoiner.add(role.getName()));
        }
        return stringJoiner.toString();
    }

}
