package com.example.demo.util;

import com.example.demo.common.Constants;

public class RedisUtil {

    public static String getUserKey(String userName) {
        return Constants.USER_INFO + userName;
    }

    public static String getCheckInKey(String userId) {
        return Constants.CHECKIN + userId;
    }

    public static String getPointLogKey(String userId) {
        return Constants.POINT_LOG + userId;
    }

}
