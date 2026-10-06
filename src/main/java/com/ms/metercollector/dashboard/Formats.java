package com.ms.metercollector.dashboard;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 템플릿에서 ${@fmt.kw(x)} 처럼 쓴다. 값이 없으면 "-".
@Component("fmt")
public class Formats {

    private static final String EMPTY = "-";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    public String kw(BigDecimal value) {
        return number(value, "#,##0.00");
    }

    public String kwh(BigDecimal value) {
        return number(value, "#,##0.000");
    }

    public String percent(BigDecimal value) {
        return number(value, "#,##0.0");
    }

    public String time(LocalDateTime value) {
        return value == null ? EMPTY : value.format(TIME);
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? EMPTY : value.format(DATE_TIME);
    }

    private static String number(BigDecimal value, String pattern) {
        if (value == null) {
            return EMPTY;
        }
        DecimalFormat format = new DecimalFormat(pattern);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}
