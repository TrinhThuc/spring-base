package com.example.demo.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataUtil.class);

    public static boolean nonEmpty(String text) {
        return !nullOrEmpty(text);
    }

    public static boolean nullOrEmpty(Collection objects) {
        return objects == null || objects.isEmpty();
    }

    public static boolean nullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean notNull(Object object) {
        return !nullObject(object);
    }

    public static boolean nullObject(Object object) {
        return object == null;
    }

    public static boolean nullOrZero(Long value) {
        return (value == null || value.equals(0L));
    }

    public static boolean nullOrZero(Integer value) {
        return (value == null || value.equals(0));
    }

    public static Long parseToLong(Object value, Long defaultVal) {
        try {
            String str = parseToString(value);
            if (nullOrEmpty(str)) {
                return null;
            }
            return Long.parseLong(str);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return defaultVal;
        }
    }

    public static Long parseToLong(Object value) {
        if (value == null) {
            return null;
        }
        return parseToLong(value, null);
    }

    public static String parseToString(Object value, String defaultVal) {
        try {
            return String.valueOf(value);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return defaultVal;
        }
    }

    public static String parseToString(Object value) {
        return parseToString(value, "");
    }

    public static boolean matchByPattern(String value, String regex) {
        if (nullOrEmpty(regex) || nullOrEmpty(value)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    public static void throwIf(boolean test, String message) throws Exception {
        if (test) throw new Exception(message);
    }
    public static <X extends Throwable> void throwIf(boolean test, Supplier<? extends X> exceptionSupplier) throws X {
        if (test) throw exceptionSupplier.get();
    }

    public static boolean nullOrEmpty(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNullOrEmpty(CharSequence cs) {
        return nullOrEmpty(cs);
    }

    public static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }


    public static String objectToJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        if (object == null) {
            return null;
        }
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(object);
    }


    private static boolean safeEqualString(String str1, String str2, boolean ignoreCase, boolean trimspace) {
        if (str1 == null || str2 == null) {
            return false;
        }

        if (trimspace) {
            str1 = str1.trim();
            str2 = str2.trim();
        }

        if (ignoreCase) {
            return str1.equalsIgnoreCase(str2);
        } else {
            return str1.equals(str2);
        }
    }

    public static boolean safeEqualString(String str1, String str2) {
        return safeEqualString(str1, str2, false, true);
    }



    public static List<String> split(String separate, String object) {
        return Optional.ofNullable(object)
                .map(x -> x.split(separate))
                .map(Arrays::asList)
                .orElseGet(ArrayList::new);
    }


    public static boolean safeEqual(Object obj1, Object obj2) {
        return ((obj1 != null) && (obj2 != null) && obj2.toString().equals(obj1.toString()));
    }

    public static void assertTrue(boolean test, String message) throws Exception {
        if (!test) throw new IllegalArgumentException(message);
    }

    public static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public static void writeToTest(byte[] data) {
        try (FileOutputStream outputStream = new FileOutputStream("/home/thiennv93/Downloads/aa/result.xlsx")) {
            outputStream.write(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static String localDateTimeToString(LocalDateTime value) {
        if (!notNull(value)) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return value.format(formatter); // "1986-04-08 12:30"
    }

}
