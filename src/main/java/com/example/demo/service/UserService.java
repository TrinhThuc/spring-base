package com.example.demo.service;


import com.example.demo.common.Constants;
import com.example.demo.common.RedisTTL;
import com.example.demo.dto.request.DeductPointRequest;
import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.PointLog;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.RedisUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class UserService {


    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final PasswordEncoder passwordEncoder;
    final ModelMapper modelMapper;

    final RedissonClient redissonClient;
    final RedisService redisService;
    final PointLogRepository pointLogRepository;

    //  @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(UserRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Optional<User> userExist = userRepository.findByUsername(request.getUsername());
        if (userExist.isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(roleRepository.findRoleByName("USER").get()));
        user.setLotusPoint(0L);
        this.userRepository.save(user);
        return modelMapper.map(user, UserResponse.class);
    }

    public UserResponse updateUser(UserRequest request, String userId) {
        Optional<User> userExist = userRepository.findById(userId);
        if (userExist.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        } else {
            User user = userExist.get();
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setDob(request.getDob());

            this.userRepository.save(user);
            return modelMapper.map(user, UserResponse.class);
        }
    }

    //@PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAllUser(String name, Pageable pageable) {
        var authen = SecurityContextHolder.getContext().getAuthentication();
        log.info("auth: " + authen.getName());

        authen.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        Page<User> users = userRepository.findUserByName(name, pageable);
        return mapEmployeeDTOsToEmpInfoResp(users);
    }

    private Page<UserResponse> mapEmployeeDTOsToEmpInfoResp(Page<User> users) {
        List<UserResponse> list = new ArrayList<UserResponse>();
        users.forEach(e -> {
            UserResponse userResponse = modelMapper.map(e, UserResponse.class);
            list.add(userResponse);
        });
        return new PageImpl<>(list, users.getPageable(), users.getTotalElements());
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User cachedUser = redisService.getValue(RedisUtil.getUserKey(name), User.class);
        if (cachedUser != null) {
            log.info("Hit cache ✅");
            return modelMapper.map(cachedUser, UserResponse.class);
        }
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
        redisService.setValue(RedisUtil.getUserKey(user.getUsername()), user, RedisTTL.USER_INFO_TTL, TimeUnit.HOURS);
        return modelMapper.map(user, UserResponse.class);
    }

    public UserResponse getById(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new AppException(ErrorCode.USER_NOT_EXISTED));
        return modelMapper.map(user, UserResponse.class);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public UserResponse deductPoint(DeductPointRequest deductPointRequest) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        int point = deductPointRequest.getPoint();
        if (point <= 0) {
            throw new AppException(ErrorCode.INVALID_POINT_VALUE);
        }

        if (user.getLotusPoint() < point) {
            throw new AppException(ErrorCode.NOT_ENOUGH_POINT);
        }
        String todayKey = RedisUtil.getCheckInKey(user.getId());
        RLock lock = redissonClient.getLock(Constants.LOCK + todayKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) throw new AppException(ErrorCode.TOO_MANY_REQUEST);
            user.setLotusPoint(user.getLotusPoint() - point);
            user = userRepository.save(user);

            PointLog pointLog = PointLog.builder()
                    .userId(user.getId())
                    .pointChanged(-point)
                    .reason(deductPointRequest.getReason() != null ? deductPointRequest.getReason() : "Trừ điểm")
                    .createdAt(LocalDateTime.now())
                    .build();
            pointLogRepository.save(pointLog);

            redisService.setValue(RedisUtil.getUserKey(user.getUsername()), user, RedisTTL.USER_INFO_TTL, TimeUnit.HOURS);
            redisService.deleteKeysByPattern(RedisUtil.getPointLogKey(user.getId()) + ":*");
            return modelMapper.map(user, UserResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
