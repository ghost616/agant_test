package com.datanew.core.toolkit;

import com.datanew.core.date.*;
import com.datanew.core.date.formate.FastDateFormat;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

public class DateKitTest {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String dateString = "2020-09-23";

    @Test
    public void testDate() throws ParseException {
        //Date类型时间转为DateTime
        //如果date本身为DateTime对象，则返回强转后的对象，否则新建一个DateTime对象
        Date date = new Date();
        System.out.println(date);
        assertNotNull(DateKit.date(date));
        // 转换为DateTime对象
        assertNotNull(DateKit.date());
        //  Long类型时间转为DateTime
        //  只支持毫秒级别时间戳，如果需要秒级别时间戳，请自行×1000
        long timeLong = 1545098699;
        System.out.println(DateKit.date(timeLong));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "1970-01-19 05:11:38";
        Date dateParse = sdf.parse(dateString);
        DateTime time = DateKit.date(dateParse);
        System.out.println(time);
        assertNotNull(DateKit.date(timeLong));
//        assertSame(DateKit.date(timeLong),time);
        // Calendar类型时间转为DateTime
        // 始终根据已有Calendar产生新的DateTime对象
        Calendar calendar = Calendar.getInstance();
        System.out.println(calendar.getTime());
        assertNotNull(DateKit.date(calendar));

    }


    @Test
    public void dateNew() {
        // 根据已有 Date 产生新的DateTime对象
        Date date = new Date();
        assertNotNull(DateKit.dateNew(date));

    }

    @Test
    public void testCalendar() {
        // 转换为Calendar对象
        Date date = new Date();
        assertNotNull(DateKit.calendar(date));


        long timeLong = 1545098699;
        assertNotNull(DateKit.calendar(timeLong));
    }

    @Test
    public void now() {
        // 当前时间，格式 yyyy-MM-dd HH:mm:ss
        assertNotNull(DateKit.now());
    }

    @Test
    public void current() {
        // 当前时间的时间戳  isNano 是否为高精度时间
        assertNotNull(DateKit.current(false));
    }

    @Test
    public void currentSeconds() {
        // 当前时间的时间戳（秒）
        assertNotNull(DateKit.currentSeconds());
    }

    @Test
    public void today() {
        // 当前日期，格式 yyyy-MM-dd
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.today(), sdf.format(cal.getTime()));
    }

    @Test
    public void year() {
        // 获得年的部分

        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.year(dateParse), 2020);
    }

    @Test
    public void quarter() {
        // 获得指定日期所属季度，从1开始计数

        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.quarter(dateParse), 3);
    }

    @Test
    public void quarterEnum() {
        //  获得指定日期所属季度

        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.quarterEnum(dateParse), Quarter.Q3);
    }

    @Test
    public void month() {
        // 获得月份，从0开始计数
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.month(dateParse), 8);
    }

    @Test
    public void monthEnum() {
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.monthEnum(dateParse), Month.SEPTEMBER);
    }

    @Test
    public void weekOfYear() {
        // 获得指定日期是所在年份的第几周
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.weekOfYear(dateParse), 39);
    }

    @Test
    public void weekOfMonth() {
        // 获得指定日期是所在月份的第几周
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.weekOfMonth(dateParse), 4);
    }

    @Test
    public void dayOfMonth() {
        // 获得指定日期是这个日期所在月份的第几天
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.dayOfMonth(dateParse), 23);
    }

    @Test
    public void dayOfWeek() {
        // 获得指定日期是星期几，1表示周日，2表示周一
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.dayOfWeek(dateParse), 4);
    }

    @Test
    public void dayOfWeekEnum() {
        // 获得指定日期是星期几，
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.dayOfWeekEnum(dateParse), Week.WEDNESDAY);
    }

    @Test
    public void hour() {
        // 获得指定日期的小时数部分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "2020-09-23 15:38:38";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.hour(dateParse, true), 15);
    }

    @Test
    public void minute() {
        // 获得指定日期的分钟数部分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "2020-09-23 15:38:38";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.minute(dateParse), 38);
    }

    @Test
    public void second() {
        // 获得指定日期的秒数部分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "2020-09-23 15:38:38";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.second(dateParse), 38);
    }

    @Test
    public void millsecond() {
        // 获得指定日期的毫秒数部分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String dateString = "2020-09-23 15:38:38:200";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.millsecond(dateParse), 200);
    }

    @Test
    public void isAM() {
        // 是否为上午
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "2020-09-23 15:38:38";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertFalse(DateKit.isAM(dateParse));
    }

    @Test
    public void isPM() {
        // 是否为下午
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = "2020-09-23 15:38:38";
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertTrue(DateKit.isPM(dateParse));
    }

    @Test
    public void thisYear() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisYear(), cal.get(Calendar.YEAR));
    }

    @Test
    public void thisMonth() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisMonth(), cal.get(Calendar.MONTH));
    }

    @Test
    public void thisMonthEnum() {
        Calendar cal = Calendar.getInstance();
        int month;
        switch (DateKit.thisMonthEnum()) {
            case JANUARY:
                month = 0;
                break;
            case FEBRUARY:
                month = 1;
                break;
            case MARCH:
                month = 2;
                break;
            case APRIL:
                month = 3;
                break;
            case MAY:
                month = 4;
                break;
            case JUNE:
                month = 5;
                break;
            case JULY:
                month = 6;
                break;
            case AUGUST:
                month = 7;
                break;
            case SEPTEMBER:
                month = 8;
                break;
            case OCTOBER:
                month = 9;
                break;
            case NOVEMBER:
                month = 10;
                break;
            case DECEMBER:
                month = 11;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + DateKit.thisMonthEnum());
        }
        assertEquals(month, cal.get(Calendar.MONTH));
    }

    @Test
    public void thisWeekOfYear() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisWeekOfYear(), cal.get(Calendar.WEEK_OF_YEAR));
    }

    @Test
    public void thisWeekOfMonth() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisWeekOfMonth(), cal.get(Calendar.WEEK_OF_MONTH));
    }

    @Test
    public void thisDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisDayOfMonth(), cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void thisDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisDayOfWeek(), cal.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void thisDayOfWeekEnum() {
        Calendar cal = Calendar.getInstance();
        int day;
        switch (DateKit.thisDayOfWeekEnum()) {
            case SUNDAY:
                day = 1;
                break;
            case SATURDAY:
                day = 7;
                break;
            case FRIDAY:
                day = 6;
                break;
            case THURSDAY:
                day = 5;
                break;
            case WEDNESDAY:
                day = 4;
                break;
            case TUESDAY:
                day = 3;
                break;
            case MONDAY:
                day = 2;
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + DateKit.thisDayOfWeekEnum());
        }
        assertEquals(day, cal.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void thisHour() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisHour(true), cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void thisMinute() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisMinute(), cal.get(Calendar.MINUTE));
    }

    @Test
    public void thisSecond() {
        Calendar cal = Calendar.getInstance();
        assertEquals(DateKit.thisSecond(), cal.get(Calendar.SECOND));
    }

    @Test
    public void thisMillsecond() {
        Calendar cal = Calendar.getInstance();
        int i1 = cal.get(Calendar.MILLISECOND);
        int i2 = DateKit.thisMillsecond();
        assertTrue((i1 - i2) < 10);
    }

    @Test
    public void testYearAndQuarter() {
        // 获得指定日期年份和季节
        // 格式：[20131]表示2013年第一季度
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assertEquals(DateKit.yearAndQuarter(dateParse), "20203");
        //获得指定日期区间内的年份和季节
        String dateString2 = "2019-09-01";
        Date dateParse2 = null;
        try {
            dateParse2 = sdf.parse(dateString2);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        LinkedHashSet<String> li = DateKit.yearAndQuarter(dateParse2, dateParse);
        System.out.println(li);
        assertNotNull(li);

        long startTime = DateKit.parse("2019-09-01").getTime();
        long endTime = DateKit.parse("2019-12-01").getTime();

        LinkedHashSet<String> li2 = DateKit.yearAndQuarter(startTime, endTime);
        System.out.println(li2);
        assertNotNull(li2);


    }

    @Test
    public void testFormat() {
        // 根据特定格式格式化日期
        Date date = DateKit.parse("20200101");
        String str = DateKit.format(date, "yyyy/MM/dd");
        System.out.println(str);
        assertEquals(str, "2020/01/01");

        String str2 = DateKit.format(date, sdf);
        System.out.println(str2);
        assertEquals(str2, "2020-01-01");
        FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd");
        String str3 = DateKit.format(date, fastDateFormat);
        assertEquals(str3, "2020-01-01");
    }

    @Test
    public void formatDateTime() {
        // 格式化日期时间
        Date date = DateKit.parse("20200101");
        assertEquals(DateKit.formatDateTime(date), "2020-01-01 00:00:00");
    }

    @Test
    public void formatDate() {
        // 格式化日期部分（不包括时间）
        Date date = DateKit.parse("20200101153223");
        assertEquals(DateKit.formatDate(date), "2020-01-01");
    }

    @Test
    public void formatTime() {
        // 格式化时间
        Date date = DateKit.parse("20200101153223");
        assertEquals(DateKit.formatTime(date), "15:32:23");
    }

    @Test
    public void formatHttpDate() {
        // 格式化为Http的标准日期格式
        Date date = DateKit.parse("20200101");
        System.out.println(DateKit.formatHttpDate(date));
        assertEquals(DateKit.formatHttpDate(date), "星期三, 01 一月 2020 00:00:00 CST");
    }

    @Test
    public void formatChineseDate() {
        // 格式化为中文日期格式，如果isUppercase为false，则返回类似：2018年10月24日，否则返回二〇一八年十月二十四日
        Date date = DateKit.parse("20200101");
        System.out.println(DateKit.formatChineseDate(date, true));
        assertEquals(DateKit.formatChineseDate(date, true), "二〇二〇年一月一日");
    }


    @Test
    public void testParse() {
        // 构建DateTime对象
        assertNotNull(DateKit.parse("2020-09-23", "yyyy-MM-dd"));
        assertNotNull(DateKit.parse("2020-09-03", sdf));
        FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd");
        assertNotNull(DateKit.parse("2020-09-03", fastDateFormat));

        assertNotNull(DateKit.parse("2020年9月23日 15时15分15秒"));
        assertNotNull(DateKit.parse("20200923151515"));
        assertNotNull(DateKit.parse("2020-09-23 15:15:15"));
        assertNotNull(DateKit.parse("2020-09-23 15:15"));
        assertNotNull(DateKit.parse("2020-09-23 15"));
        assertNotNull(DateKit.parse("2020-09-23"));
        assertNotNull(DateKit.parse("2020-09"));
        assertNotNull(DateKit.parse("2020"));


    }


    @Test
    public void parseDateTime() {
        // 格式yyyy-MM-dd HH:mm:ss
        assertEquals(DateKit.parse("2020-09-23 15:15:15"), DateKit.parseDateTime("2020年9月23日 15时15分15秒"));
    }

    @Test
    public void parseDate() {
        // 格式yyyy-MM-dd
        assertEquals(DateKit.parse("2020-09-23 00:00:00"), DateKit.parseDate("2020-09-23"));
        assertEquals(DateKit.parse("2020-09-23 00:00:00"), DateKit.parseDate("2020年9月23日"));
    }

    @Test
    public void parseTime() {
        // 解析时间，格式HH:mm:ss，默认为1970-01-01
        System.out.println(DateKit.parseTime("12:21:21"));
        assertNotNull(DateKit.parseTime("12:21:21"));
    }

    @Test
    public void parseTimeToday() {
        // 解析时间，格式HH:mm:ss，日期默认为今天
        System.out.println(DateKit.parseTimeToday("12:21:21"));
        assertNotNull(DateKit.parseTimeToday("12:21:21"));
    }

    @Test
    public void parseUTC() {
        // 解析UTC时间，格式为：yyyy-MM-dd'T'HH:mm:ss'Z
        System.out.println(DateKit.parseUTC("2020-09-01T12:21:21Z"));
        assertNotNull(DateKit.parseUTC("2020-09-01T12:21:21Z"));
    }

    @Test
    public void testBeginOfDay() {
        // 获取某天的开始时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.beginOfDay(dateParse));
        assertNotNull(DateKit.beginOfDay(dateParse));
    }

    @Test
    public void testEndOfDay() {
        // 获取某天的结束时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.endOfDay(dateParse));
        assertNotNull(DateKit.endOfDay(dateParse));
    }

    @Test
    public void testBeginOfWeek() {
        // 获取某周的开始时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.beginOfWeek(dateParse));
        assertNotNull(DateKit.beginOfWeek(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.beginOfWeek(calendar));
        assertNotNull(DateKit.beginOfWeek(calendar));
    }


    @Test
    public void testEndOfWeek() {
        // 获取某周的结束时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.endOfWeek(dateParse));
        assertNotNull(DateKit.endOfWeek(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.endOfWeek(calendar, true).getTime());
        assertNotNull(DateKit.endOfWeek(calendar));
    }

    @Test
    public void testBeginOfMonth() {
        // 获取某月的开始时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.beginOfMonth(dateParse));
        assertNotNull(DateKit.beginOfMonth(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.beginOfMonth(calendar).getTime());
        assertNotNull(DateKit.beginOfMonth(calendar));
    }

    @Test
    public void testEndOfMonth() {
        // 获取某月的结束时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.endOfMonth(dateParse));
        assertNotNull(DateKit.endOfMonth(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.endOfMonth(calendar).getTime());
        assertNotNull(DateKit.endOfMonth(calendar));
    }

    @Test
    public void testBeginOfQuarter() {
        // 获取某季度的开始时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.beginOfQuarter(dateParse));
        assertNotNull(DateKit.beginOfQuarter(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.beginOfQuarter(calendar).getTime());
        assertNotNull(DateKit.beginOfQuarter(calendar));
    }

    @Test
    public void testEndOfQuarter() {
        // 获取某季度的结束时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.endOfQuarter(dateParse));
        assertNotNull(DateKit.endOfQuarter(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.endOfQuarter(calendar).getTime());
        assertNotNull(DateKit.endOfQuarter(calendar));
    }


    @Test
    public void testBeginOfYear() {
        // 获取某年的开始时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.beginOfYear(dateParse));
        assertNotNull(DateKit.beginOfYear(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.beginOfYear(calendar).getTime());
        assertNotNull(DateKit.beginOfYear(calendar));
    }

    @Test
    public void testEndOfYear() {
        // 获取某年的结束时间
        Date dateParse = null;
        try {
            dateParse = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(DateKit.endOfYear(dateParse));
        assertNotNull(DateKit.endOfYear(dateParse));

        Calendar calendar = Calendar.getInstance();
        System.out.println(DateKit.endOfYear(calendar).getTime());
        assertNotNull(DateKit.endOfYear(calendar));
    }

    @Test
    public void yesterday() {
        System.out.println(DateKit.yesterday());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, -1);
        assertNotNull(DateKit.yesterday());
        assertTrue(DateKit.isSameDay(DateKit.yesterday(), DateKit.parse(sdf.format(cal.getTime()))));
    }

    @Test
    public void tomorrow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        System.out.println(DateKit.tomorrow());
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        assertTrue(DateKit.isSameDay(DateKit.tomorrow(), DateKit.parse(sdf.format(cal.getTime()))));
    }

    @Test
    public void lastWeek() {
        System.out.println(DateKit.lastWeek());
        assertNotNull(DateKit.lastWeek());
    }

    @Test
    public void nextWeek() {
        System.out.println(DateKit.nextWeek());
        assertNotNull(DateKit.nextWeek());
    }

    @Test
    public void lastMonth() {
        System.out.println(DateKit.lastMonth());
        assertNotNull(DateKit.lastMonth());
    }

    @Test
    public void nextMonth() {
        System.out.println(DateKit.nextMonth());
        assertNotNull(DateKit.nextMonth());
    }

    @Test
    public void offsetMillisecond() {
        // 偏移毫秒数
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetMillisecond(dateParse, 10000));
        Date ret = DateKit.parse("2020-09-23 09:23:33");
        assertEquals(DateKit.offsetMillisecond(dateParse, 10000), ret);
    }

    @Test
    public void offsetSecond() {
        // 偏移秒数
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetSecond(dateParse, 60));
        Date ret = DateKit.parse("2020-09-23 09:24:23");
        assertEquals(DateKit.offsetSecond(dateParse, 60), ret);
    }

    @Test
    public void offsetMinute() {
        // 偏移分钟
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetMinute(dateParse, 1));
        Date ret = DateKit.parse("2020-09-23 09:24:23");
        assertEquals(DateKit.offsetMinute(dateParse, 1), ret);
    }

    @Test
    public void offsetHour() {
        // 偏移小时
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetHour(dateParse, 1));
        Date ret = DateKit.parse("2020-09-23 10:23:23");
        assertEquals(DateKit.offsetHour(dateParse, 1), ret);
    }

    @Test
    public void offsetDay() {
        // 偏移天
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetDay(dateParse, 1));
        Date ret = DateKit.parse("2020-09-24 09:23:23");
        assertEquals(DateKit.offsetDay(dateParse, 1), ret);
    }

    @Test
    public void offsetWeek() {
        // 偏移周
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetWeek(dateParse, 1));
        Date ret = DateKit.parse("2020-09-30 09:23:23");
        assertEquals(DateKit.offsetWeek(dateParse, 1), ret);
    }

    @Test
    public void offsetMonth() {
        // 偏移月
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
        System.out.println(DateKit.offsetMonth(dateParse, 1));
        Date ret = DateKit.parse("2020-10-23 09:23:23");
        assertEquals(DateKit.offsetMonth(dateParse, 1), ret);
    }

    @Test
    public void offset() {
        // 获取指定日期偏移指定时间后的时间
        Date dateParse = DateKit.parse("2020-09-23 09:23:23");

        System.out.println(DateKit.offset(dateParse, DateField.YEAR, -1));
        Date ret = DateKit.parse("2019-09-23 09:23:23");
        assertEquals(DateKit.offset(dateParse, DateField.YEAR, -1), ret);

        System.out.println(DateKit.offset(dateParse, DateField.MONTH, -1));
        Date ret2 = DateKit.parse("2020-08-23 09:23:23");
        assertEquals(DateKit.offset(dateParse, DateField.MONTH, -1), ret2);

        System.out.println(DateKit.offset(dateParse, DateField.DAY_OF_YEAR, -1));
        Date ret3 = DateKit.parse("2020-09-22 09:23:23");
        assertEquals(DateKit.offset(dateParse, DateField.DAY_OF_YEAR, -1), ret3);

        System.out.println(DateKit.offset(dateParse, DateField.HOUR, -1));
        Date ret4 = DateKit.parse("2020-09-23 08:23:23");
        assertEquals(DateKit.offset(dateParse, DateField.HOUR, -1), ret4);

        System.out.println(DateKit.offset(dateParse, DateField.MINUTE, -1));
        Date ret5 = DateKit.parse("2020-09-23 09:22:23");
        assertEquals(DateKit.offset(dateParse, DateField.MINUTE, -1), ret5);

        System.out.println(DateKit.offset(dateParse, DateField.SECOND, -1));
        Date ret6 = DateKit.parse("2020-09-23 09:23:22");
        assertEquals(DateKit.offset(dateParse, DateField.SECOND, -1), ret6);

        System.out.println(DateKit.offset(dateParse, DateField.MILLISECOND, 1000));
        Date ret7 = DateKit.parse("2020-09-23 09:23:24");
        assertEquals(DateKit.offset(dateParse, DateField.MILLISECOND, 1000), ret7);

    }

    @Test
    public void testBetween() {
        // 判断两个日期相差的时长，只保留绝对值
        Date startDate = DateKit.parse("2020-09-30");
        Date endDate = DateKit.parse("2020-09-01");
        System.out.println(DateKit.between(startDate, endDate, DateUnit.DAY));
        assertEquals(DateKit.between(startDate, endDate, DateUnit.DAY), 29);
        System.out.println(DateKit.between(startDate, endDate, DateUnit.WEEK, false));
        assertEquals(DateKit.between(startDate, endDate, DateUnit.WEEK, false), -4);

    }

    @Test
    public void betweenMs() {
        // 判断两个日期相差的毫秒数
        Date startDate = DateKit.parse("2020-09-23 12:12:12.3242");
        Date endDate = DateKit.parse("2020-09-23 12:12:13.3242");
        System.out.println(DateKit.betweenMs(startDate, endDate));
        assertEquals(DateKit.betweenMs(startDate, endDate), 1000);
    }

    @Test
    public void betweenDay() {
        // 判断两个日期相差的天数
        Date startDate = DateKit.parse("2020-09-23 23:59:59");
        Date endDate = DateKit.parse("2020-09-24 00:00:00");
        System.out.println(DateKit.betweenDay(startDate, endDate, true));
        assertEquals(DateKit.betweenDay(startDate, endDate, true), 1);

        System.out.println(DateKit.betweenDay(startDate, endDate, false));
        assertEquals(DateKit.betweenDay(startDate, endDate, false), 0);
    }

    @Test
    public void betweenMonth() {
        // 计算两个日期相差月数
        Date startDate = DateKit.parse("2020-08-23");
        Date endDate = DateKit.parse("2020-09-24");
        System.out.println(DateKit.betweenMonth(startDate, endDate, true));
        assertEquals(DateKit.betweenMonth(startDate, endDate, true), 1);
        Date startDate1 = DateKit.parse("2020-09-01");
        Date endDate1 = DateKit.parse("2020-09-24");
        System.out.println(DateKit.betweenMonth(startDate1, endDate1, false));
        assertEquals(DateKit.betweenMonth(startDate1, endDate1, false), 0);
    }

    @Test
    public void betweenYear() {
        // 计算两个日期相差年数
        Date startDate = DateKit.parse("2019-10-23");
        Date endDate = DateKit.parse("2020-09-24");
        System.out.println(DateKit.betweenYear(startDate, endDate, true));
        assertEquals(DateKit.betweenYear(startDate, endDate, true), 1);
        Date startDate1 = DateKit.parse("2020-01-01");
        Date endDate1 = DateKit.parse("2020-09-24");
        System.out.println(DateKit.betweenYear(startDate1, endDate1, false));
        assertEquals(DateKit.betweenYear(startDate1, endDate1, false), 0);
    }

    @Test
    public void formatBetween() {
        // 格式化日期间隔输出
        Date startDate = DateKit.parse("2019-10-23");
        Date endDate = DateKit.parse("2020-09-24");
        // Day
        System.out.println(DateKit.formatBetween(startDate, endDate, BetweenFormater.Level.DAY));
        assertNotNull(DateKit.formatBetween(startDate, endDate, BetweenFormater.Level.DAY));
        // Hour
        Date startDate1 = DateKit.parse("2020-10-23 12:12:12");
        Date endDate1 = DateKit.parse("2020-10-24 20:13:13");
        System.out.println(DateKit.formatBetween(startDate1, endDate1, BetweenFormater.Level.HOUR));
        assertNotNull(DateKit.formatBetween(startDate1, endDate1, BetweenFormater.Level.HOUR));
        // Minute
        Date startDate2 = DateKit.parse("2020-10-23 12:12:12");
        Date endDate2 = DateKit.parse("2020-10-24 13:59:13");
        System.out.println(DateKit.formatBetween(startDate2, endDate2, BetweenFormater.Level.MINUTE));
        assertNotNull(DateKit.formatBetween(startDate2, endDate2, BetweenFormater.Level.MINUTE));
        // Second
        Date startDate3 = DateKit.parse("2020-10-23 12:12:12");
        Date endDate3 = DateKit.parse("2020-10-24 13:59:59");
        System.out.println(DateKit.formatBetween(startDate3, endDate3, BetweenFormater.Level.SECOND));
        assertNotNull(DateKit.formatBetween(startDate3, endDate3, BetweenFormater.Level.SECOND));
        // MillSecond
        long startDate4 = DateKit.parse("2020-10-23").getTime();
        System.out.println(DateKit.formatBetween(startDate4, BetweenFormater.Level.MILLSECOND));
        assertNotNull(DateKit.formatBetween(startDate4, BetweenFormater.Level.MILLSECOND));
    }

    @Test
    public void isIn() {
        // 当前日期是否在日期指定范围内
        // 起始日期和结束日期可以互换

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        // -1昨天，0今天，1明天
        calendar.add(Calendar.DATE, 1);
        Date tomorrow = calendar.getTime();
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -1);
        Date yesterday = calendar1.getTime();

        assertTrue(DateKit.isIn(today, yesterday, tomorrow));

    }

    @Test
    public void isSameTime() {
        // 是否为相同时间
        Date startDate = DateKit.parse("23:59:59");
        Date endDate = DateKit.parse("23:59:59");
        assertTrue(DateKit.isSameTime(startDate, endDate));
    }


    @Test
    public void testIsSameDay() {
        // 比较两个日期是否为同一天
        Date startDate = DateKit.parse("2020-02-02");
        Date endDate = DateKit.parse("2020-02-02");
        assertTrue(DateKit.isSameTime(startDate, endDate));

        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.SEPTEMBER, 23, 0, 0, 0);
        Date date = DateKit.parse("2020-09-23 00:00:00");
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date);
        System.out.println(calendar.getTime());
        System.out.println(calendar2.getTime());
        assertTrue(DateKit.isSameDay(calendar, calendar2));

    }

    @Test
    public void spendNt() {
        // 计时，常用于记录某段代码的执行时间，单位：纳秒
        long preTime = DateKit.parse("2020-09-24 11:01:23").getTime();
        System.out.println(DateKit.spendNt(preTime));
        assertNotNull(DateKit.spendNt(preTime));
    }

    @Test
    public void spendMs() {
        // 计时，常用于记录某段代码的执行时间，单位：毫秒
        long preTime = DateKit.parse("2020-09-24 11:01:23").getTime();
        System.out.println(DateKit.spendMs(preTime));
        assertNotNull(DateKit.spendMs(preTime));
    }

    @Test
    public void toIntSecond() throws ParseException {
        // 格式化成yyMMddHHmm后转换为int型
        Date date = sdf.parse("2020-09-24 12:20:12");
        System.out.println(DateKit.toIntSecond(date));
        assertNotNull(DateKit.toIntSecond(date));
    }

    @Test
    public void weekCount() {
        // 计算指定指定时间区间内的周数
        Date startDate = DateKit.parse("2020-02-02");
        Date endDate = DateKit.parse("2020-03-02");
        System.out.println(DateKit.weekCount(startDate, endDate));
        assertEquals(DateKit.weekCount(startDate, endDate), 5);
    }

    @Test
    public void timer() {
        // 计时器
        // 计算某个过程花费的时间，精确到毫秒
        TimeInterval timeInterval = DateKit.timer();

        assertNotNull(timeInterval);
    }

    @Test
    public void testAgeOfNow() throws ParseException {
        // 生日转为年龄，计算法定年龄
        System.out.println(DateKit.ageOfNow("1999-01-01"));
        assertEquals(DateKit.ageOfNow("1999-01-01"), 21);

        Date date = sdf.parse("1999-01-01");
        System.out.println(DateKit.ageOfNow(date));
        assertEquals(DateKit.ageOfNow("1999-01-01"), 21);

    }

    @Test
    public void age() throws ParseException {
        // 计算相对于dateToCompare的年龄，长用于计算指定生日在某年的年龄
        Date date = sdf.parse("1999-08-08");
        Date date2 = sdf.parse("2008-08-07");

        System.out.println(DateKit.age(date, date2));
        assertEquals(DateKit.age(date, date2), 8);

    }

    @Test
    public void isLeapYear() {
        // 是否闰年
        assertTrue(DateKit.isLeapYear(2020));
    }

    @Test
    public void isExpired() {
        // 判定给定开始时间经过某段时间后是否过期
        Date startDate = DateKit.parse("2020-01-01");
        Date checkDate = DateKit.parse("2020-09-24");

        assertFalse(DateKit.isExpired(startDate, DateField.MONTH, 5, checkDate));
    }

    @Test
    public void timeToSecond() {
        // HH:mm:ss 时间格式字符串转为秒数
        String str = "20:20:20";
        int time = DateKit.timeToSecond(str);
        System.out.println(time);
        assertNotNull(time);
    }

    @Test
    public void secondToTime() {
        // 秒数转为时间格式(HH:mm:ss)
        int time = 73220;
        String str = DateKit.secondToTime(time);
        System.out.println(str);
        assertNotNull(str);
    }

    @Test
    public void range() {
        // 创建日期范围生成器  unit 步进单位
        Date startDate = DateKit.parse("2020-08-01");
        Date endDate = DateKit.parse("2020-09-24");
        DateRange dateRange = DateKit.range(startDate, endDate, DateField.HOUR);
        assertNotNull(dateRange);
    }

    @Test
    public void rangeToList() {
        // 创建日期范围生成器
        Date startDate = DateKit.parse("2020-08-01");
        Date endDate = DateKit.parse("2020-09-24");
        List<DateTime> dateTimeList = DateKit.rangeToList(startDate, endDate, DateField.HOUR);
        assertNotNull(dateTimeList);
    }
}