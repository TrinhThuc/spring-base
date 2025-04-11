package com.example.demo.service.impl;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.demo.util.GsonUtil;
import com.google.gson.*;
import org.modelmapper.ModelMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.demo.util.RedisUtil;
import com.google.gson.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

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
    private final Gson gson = GsonUtil.getGson();

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CheckinLogResponse checkin() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = redisService.getValue(RedisUtil.getUserKey(name), User.class);
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

        String todayKey = RedisUtil.getCheckInKey(user.getId());
        RLock lock = redissonClient.getLock(Constants.LOCK + todayKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) throw new AppException(ErrorCode.TOO_MANY_REQUEST);
            if (Boolean.TRUE.equals(redisService.hasKey(todayKey))) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
            }

            String monthKey = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Optional<CheckinLog> optionalLog = checkinLogRepository.findByUserIdAndMonthKey(user.getId(), monthKey);
            List<String> checkinList;
            CheckinLog log;

            if (optionalLog.isPresent()) {
                log = optionalLog.get();
                checkinList = gson.fromJson(log.getCheckins(), new TypeToken<List<String>>() {}.getType());
                if (checkinList.contains(today.toString())) {
                    throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
                }
            } else {
                checkinList = new ArrayList<>();
                log = new CheckinLog();
                log.setUserId(user.getId());
                log.setMonthKey(monthKey);
            }

            if (checkinList.size() >= getMaxCheckinsPerMonth()) {
                throw new AppException(ErrorCode.CHECKIN_LIMIT_REACHED);
            }

            checkinList.add(now.toString());
            log.setCheckins(gson.toJson(checkinList));
            log.setUpdatedAt(now);
            checkinLogRepository.save(log);

            int point = getPointForDay(checkinList.size());

            pointLogRepository.save(PointLog.builder()
                    .userId(user.getId())
                    .pointChanged(point)
                    .reason("Checkin ngày " + today)
                    .build());

            user.setLotusPoint(user.getLotusPoint() + point);
            userRepository.save(user);

            CheckinLogResponse response = new CheckinLogResponse();
            response.setTotalCheckins(checkinList.size());

            redisService.setValue(RedisUtil.getUserKey(user.getUsername()), user, RedisTTL.USER_INFO_TTL, TimeUnit.HOURS);
            redisService.setValue(todayKey, "checked", Duration.between(now, today.plusDays(1).atStartOfDay()).getSeconds(), TimeUnit.SECONDS);
            redisService.deleteKeysByPattern(RedisUtil.getPointLogKey(user.getId()) + ":*");

            return response;

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
        User user = redisService.getValue(RedisUtil.getUserKey(name), User.class);
        if (user == null) {
            user = userRepository.findByUsername(name).orElseThrow(
                    () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        String monthKey = yearMonth.toString();

        Optional<CheckinLog> optionalLog = checkinLogRepository.findByUserIdAndMonthKey(user.getId(), monthKey);
        Map<LocalDate, LocalDateTime> checkinMap = new HashMap<>();

        if (optionalLog.isPresent()) {
            String checkinsJson = optionalLog.get().getCheckins();
            if (checkinsJson != null && !checkinsJson.isBlank()) {
                try {
                    List<LocalDateTime> datetimes = gson.fromJson(checkinsJson, new TypeToken<List<LocalDateTime>>() {}.getType());
                    for (LocalDateTime dt : datetimes) {
                        checkinMap.put(dt.toLocalDate(), dt);
                    }
                } catch (JsonSyntaxException e) {
                    throw new AppException(ErrorCode.SYSTEM_ERROR);
                }
            }
        }

        List<CheckinStatusResponse> responses = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            LocalDateTime checkinDatetime = checkinMap.get(day);
            responses.add(new CheckinStatusResponse(
                    day,
                    checkinDatetime != null,
                    checkinDatetime
            ));
        }

        return responses;
    }


    @Override
    public Page<PointLogResponse> getPointLogs(int page, int size) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = redisService.getValue(RedisUtil.getUserKey(username), User.class);
        if (user == null) {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        String cacheKey = RedisUtil.getPointLogKey(user.getId()) + ":" + page + ":" + size;

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
