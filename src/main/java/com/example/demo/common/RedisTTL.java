package com.example.demo.common;

import java.util.concurrent.TimeUnit;

public class RedisTTL {
    public static final long USER_INFO_TTL = 2;
    public static final long CHECKIN_TTL = 1;
    public static final long POINT_LOG_TTL = 10;

    public static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;
}
