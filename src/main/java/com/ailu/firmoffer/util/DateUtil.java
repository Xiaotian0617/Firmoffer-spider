package com.ailu.firmoffer.util;

import org.springframework.context.annotation.Configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * @author mr.wang
 * @version V1.0
 * @Description NOTE:
 * @par 版权信息：
 * 2018 Copyright 河南艾鹿网络科技有限公司 All Rights Reserved.
 */
@Configuration
public interface DateUtil {

    /**
     * 计算账户时用到的 时间常量 最初
     */
    static Date getFirst() {
        return LocalDateTimeToDate(getMinTime());
    }


    /**
     * 计算账户时用到的 时间常量 最晚
     */
    static Date getLast() {
        return LocalDateTimeToDate(getNowTime().plusDays(1));
    }


    /**
     * 计算账户时用到的 时间常量 今日最初
     */
    static Date getTodayStart() {
        return LocalDateTimeToDate(getStartTime());
    }

    /**
     * 计算账户时用到的 时间常量 今日最初
     */
    static Date getTodayLast() {
        return LocalDateTimeToDate(getNowTime().plusDays(1));
    }

    /**
     * 计算账户时用到的 时间常量 昨日最初
     */
    static Date getYesterdayStart() {
        return LocalDateTimeToDate(getYesterdayStartTime());
    }

    /**
     * 计算账户时用到的 时间常量 昨日最初
     */
    static Date getYesterdayLast() {
        return LocalDateTimeToDate(getYesterdayEndTime());
    }


    /**
     * 计算账户时用到的 时间常量 一周最初
     */
    static Date getWeekStart() {
        return LocalDateTimeToDate(getWeekStartTime());
    }


    /**
     * 计算账户时用到的 时间常量 一周最晚
     */
    static Date getWeekLast() {
        return LocalDateTimeToDate(getWeekEndTime().plusDays(1));
    }

    /**
     * 计算账户时用到的 时间常量 一周最晚
     */
    static Date getMonthStart() {
        return LocalDateTimeToDate(getMonthStartTime());
    }


    /**
     * 计算账户时用到的 时间常量 一月最晚
     */
    static Date getMonthLast() {
        return LocalDateTimeToDate(getMonthEndTime().plusDays(1));
    }


    static LocalDateTime getNowTime() {
        return LocalDateTime.now();
    }

    static LocalDateTime getMinTime() {
        return LocalDateTime.of(1970, 1, 1, 0, 0, 0);
    }

    static LocalDateTime getStartTime() {
        LocalDateTime startTime = LocalDateTime.of(getNowTime().getYear(), getNowTime().getMonth(), getNowTime().getDayOfMonth(), 0, 0, 0);
        return startTime;
    }

    static LocalDateTime getEndTime() {
        LocalDateTime endTime = LocalDateTime.of(getNowTime().getYear(), getNowTime().getMonth(), getNowTime().getDayOfMonth() + 1, 0, 0, 0);
        return endTime;
    }

    static LocalDateTime getYesterdayStartTime() {
        LocalDateTime yesterdayStartTime = getStartTime().minusDays(1);
        return yesterdayStartTime;
    }

    static LocalDateTime getYesterdayEndTime() {
        LocalDateTime yesterdayEndTime = getEndTime().minusDays(1);
        return yesterdayEndTime;
    }

    static LocalDateTime getWeekStartTime() {
        LocalDateTime weekStartTime = getEndTime().minusWeeks(1);
        return weekStartTime;
    }

    static LocalDateTime getWeekEndTime() {
        //LocalDateTime weekEndTime = getEndTime().minusWeeks(1);
        return getNowTime();
    }

    static LocalDateTime getMonthStartTime() {
        LocalDateTime monthStartTime = getEndTime().minusMonths(1);
        return monthStartTime;
    }

    static LocalDateTime getMonthEndTime() {
        //LocalDateTime monthEndTime = getEndTime().minusMonths(1);
        return getNowTime();
    }

    static Date LocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    static Date transForDate(Long time) {
        if (Objects.equals(null, time)) {
            return new Date();
        }
        return transForDate(String.valueOf(time));
    }

    static Date transForDate(String time) {
        if (Objects.equals(null, time) || Objects.equals("", time)) {
            return new Date();
        }
        Long timeL = Long.valueOf(time);
        long msl = timeL * 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date temp = null;
        if (!Objects.equals(null, time)) {
            try {
                String str = sdf.format(msl);
                temp = sdf.parse(str);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return temp;
    }

    public static void main(String[] args) {
        System.out.println(getFirst());
        System.out.println(getTodayStart());
        System.out.println(getWeekStart());
        System.out.println(getTodayLast());
//        System.out.println(FIRST);
//        System.out.println(LAST);
    }


}
