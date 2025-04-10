package com.example.demo.service;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

public interface RedisService {
    <T> T getValue(String key, Class<T> classOutput);

    <T> T getValue(String key, Type typeOfT);

    <T> void setValue(String key, T value, long timeout, TimeUnit unit);

    <T> void setValue(String key, T data, Type typeOfT, long timeout, TimeUnit unit);

    boolean hasKey(String key);

    void deleteKeysByPattern(String pattern);
}
