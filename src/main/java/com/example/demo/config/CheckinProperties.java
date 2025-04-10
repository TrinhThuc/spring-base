package com.example.demo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "checkin")
@Data
public class CheckinProperties {
    private List<String> allowedTimeRanges;
    private Map<Integer, Integer> pointAwards;
    private int maxCheckinsPerMonth;

    public List<TimeRange> getParsedTimeRanges() {
        return allowedTimeRanges.stream()
                .map(TimeRange::parse)
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class TimeRange {
        private LocalTime start;
        private LocalTime end;

        public static TimeRange parse(String range) {
            String[] parts = range.split("-");
            return new TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
        }

        public boolean isWithin(LocalTime time) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
    }
}
