package com.techfork.global.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class StringToLocalDateTimeConverter {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'"),
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    public LocalDateTime convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                // ISO_LOCAL_DATE 포맷인 경우 LocalDate로 파싱 후 LocalDateTime으로 변환
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    LocalDate date = LocalDate.parse(source, formatter);
                    return date.atStartOfDay();
                }
                return LocalDateTime.parse(source, formatter);
            } catch (DateTimeParseException e) {
                // 다음 포맷 시도
            }
        }

        throw new IllegalArgumentException("Unable to parse date: " + source);
    }
}
