package com.example.demo.service.impl;


import com.example.demo.service.RedisService;
import com.example.demo.util.DataUtil;
import com.example.demo.util.GsonUtil;
import com.google.gson.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    private ValueOperations<String, String> valueOps;
    private static final long EXPIRE = 30;

    private final Gson gson = GsonUtil.getGson();

    @PostConstruct
    private void init() {
        valueOps = redisTemplate.opsForValue();
    }

    @Override
    public <T> T getValue(String key, Class<T> classOutput) {
        if (DataUtil.nullOrEmpty(key)) {
            return null;
        }
        String value = valueOps.get(key);
        if (DataUtil.nullOrEmpty(value)) {
            return null;
        }
        return gson.fromJson(value, classOutput);
    }

    @Override
    public <T> T getValue(String key, Type typeOfT) {
        if (DataUtil.nullOrEmpty(key)) {
            return null;
        }
        String value = valueOps.get(key);
        if (DataUtil.nullOrEmpty(value)) {
            return null;
        }
        return gson.fromJson(value, typeOfT);
    }

    @Override
    public <T> void setValue(String key, T value, long timeout, TimeUnit unit) {
        if (DataUtil.nullOrEmpty(key) || value == null) {
            return;
        }
        String jsonValue = gson.toJson(value);
        valueOps.set(key, jsonValue, timeout, unit);
        log.debug("Set key {} to redis with TTL {} {}", key, timeout, unit);
    }

    @Override
    public <T> void setValue(String key, T data, Type typeOfT, long timeout, TimeUnit unit) {
        if (DataUtil.nullOrEmpty(key) || data == null) {
            return;
        }
        String json = gson.toJson(data, typeOfT);
        valueOps.set(key, json, timeout, unit);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteKeysByPattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> matchedKeys = keys.getKeysByPattern(pattern);

        Iterator<String> iterator = matchedKeys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            redissonClient.getBucket(key).delete();
        }
    }
}
