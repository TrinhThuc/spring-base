package com.example.demo.service.impl;

import com.example.demo.common.Constants;
import com.example.demo.common.RedisTTL;
import com.example.demo.config.CheckinProperties;
import com.example.demo.dto.response.CheckinLogResponse;
import com.example.demo.dto.response.CheckinStatusResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.PointLogResponse;
import com.example.demo.entity.CheckinLog;
import com.example.demo.entity.PointLog;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CheckinLogRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.RedisService;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final RedisService redisService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final CheckinLogRepository checkinLogRepository;
    private final CheckinProperties checkinProperties;
    private final RedissonClient redissonClient;
    private final PointLogRepository pointLogRepository;

    @Override
    @Transactional
    public CheckinLogResponse checkin() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = redisService.getValue(Constants.USER_INFO + name, User.class);
        if (user == null) {
            user = userRepository.findByUsername(name).orElseThrow(
                    () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        LocalTime nowTime = LocalTime.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        if (!isWithinAllowedTime(nowTime)) {
            throw new AppException(ErrorCode.CHECKIN_NOT_IN_ALLOWED_TIME);
        }

        String todayKey = Constants.CHECKIN + user.getId() + ":" + LocalDate.now();
        RLock lock = redissonClient.getLock(Constants.LOCK + todayKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) throw new AppException(ErrorCode.TOO_MANY_REQUEST);
            if (Boolean.TRUE.equals(redisService.hasKey(todayKey))) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
            }
            boolean alreadyCheckedIn = checkinLogRepository.existsByUserIdAndCheckinDatetimeBetween(
                    user.getId(),
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            );
            if (alreadyCheckedIn) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
            }

            String monthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            int countInMonth = checkinLogRepository.countByUserIdAndMonthKey(user.getId(), monthKey);
            if (countInMonth >= this.getMaxCheckinsPerMonth()) {
                throw new AppException(ErrorCode.CHECKIN_LIMIT_REACHED);
            }

            int point = getPointForDay(countInMonth + 1);

            CheckinLog checkinLog = CheckinLog.builder()
                    .userId(user.getId())
                    .checkinDatetime(now)
                    .pointAwarded(point)
                    .monthKey(monthKey)
                    .build();

            checkinLog = checkinLogRepository.save(checkinLog);
            pointLogRepository.save(PointLog.builder()
                    .userId(user.getId())
                    .pointChanged(point)
                    .reason("Checkin ngày " + today)
                    .build());

            user.setLotusPoint(user.getLotusPoint() + point);
            userRepository.save(user);
            CheckinLogResponse checkinLogResponse = modelMapper.map(checkinLog, CheckinLogResponse.class);
            checkinLogResponse.setTotalCheckins(countInMonth + 1);
            redisService.setValue(Constants.USER_INFO + user.getUsername(), user, RedisTTL.USER_INFO_TTL, TimeUnit.HOURS);
            redisService.setValue(todayKey, "checked", Duration.between(now, today.plusDays(1).atStartOfDay()).getSeconds(), TimeUnit.SECONDS);
            redisService.deleteKeysByPattern(Constants.POINT_LOG + user.getId() + ":*");
            return checkinLogResponse;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<CheckinStatusResponse> getCheckinStatuses(int year, int month) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = redisService.getValue(Constants.USER_INFO + name, User.class);
        if (user == null) {
            user = userRepository.findByUsername(name).orElseThrow(
                    () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<CheckinLog> checkinLogs = checkinLogRepository.findByUserIdAndCheckinDatetimeBetween(
                user.getId(), start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        Map<LocalDate, CheckinLog> logMap = checkinLogs.stream()
                .collect(Collectors.toMap(
                        checkinLog -> checkinLog.getCheckinDatetime().toLocalDate(),
                        Function.identity()
                ));

        List<CheckinStatusResponse> responses = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            CheckinLog log = logMap.get(day);
            responses.add(new CheckinStatusResponse(
                    day,
                    log != null,
                    log != null ? log.getCheckinDatetime() : null,
                    log != null ? log.getPointAwarded() : null
            ));
        }

        return responses;
    }

    @Override
    public Page<PointLogResponse> getPointLogs(int page, int size) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = redisService.getValue(Constants.USER_INFO + username, User.class);
        if (user == null) {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        String cacheKey = Constants.POINT_LOG + user.getId() + ":" + page + ":" + size;

        Type pageType = new TypeToken<PageResponse<PointLogResponse>>() {
        }.getType();
        PageResponse<PointLogResponse> cached = redisService.getValue(cacheKey, pageType);
        if (cached != null) {
            log.info("✅ Hit cache with key: {}", cacheKey);
            return new PageImpl<>(cached.getContent(), PageRequest.of(cached.getPage(), cached.getSize()), cached.getTotalElements());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PointLog> pointLogs = pointLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<PointLogResponse> content = pointLogs.stream()
                .map(p -> modelMapper.map(p, PointLogResponse.class))
                .collect(Collectors.toList());

        PageImpl<PointLogResponse> response = new PageImpl<>(content, pageable, pointLogs.getTotalElements());

        PageResponse<PointLogResponse> cacheData = new PageResponse<>(content, page, size, pointLogs.getTotalElements());
        redisService.setValue(cacheKey, cacheData, pageType, RedisTTL.POINT_LOG_TTL, TimeUnit.MINUTES);
        return response;
    }


    public boolean isWithinAllowedTime(LocalTime now) {
        return checkinProperties.getParsedTimeRanges().stream()
                .anyMatch(r -> r.isWithin(now));
    }

    public int getPointForDay(int checkinDay) {
        return checkinProperties.getPointAwards()
                .getOrDefault(checkinDay, 0);
    }

    public int getMaxCheckinsPerMonth() {
        return checkinProperties.getMaxCheckinsPerMonth();
    }
}
