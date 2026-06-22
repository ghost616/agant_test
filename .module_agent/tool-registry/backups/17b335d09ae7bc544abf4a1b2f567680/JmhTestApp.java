package com.datanew.core.toolkit;


import com.datanew.core.date.DateField;
import com.datanew.core.date.DateTime;
import com.datanew.core.date.DateUnit;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@State(Scope.Thread)
public class JmhTestApp {

//    LocalDateTime getLocal(String str) {
//        return getLocal(str, "yyyy-MM-dd mm:HH:ss");
//    }
//
//    LocalDateTime getLocal(String str, String pattern) {
//        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
//        return LocalDateTime.parse(str, df);
//    }
//
//    LocalDate getLocalDate(String str) {
//        return getLocalDate(str, "yyyy-MM-dd");
//    }
//
//    LocalDate getLocalDate(String str, String pattern) {
//        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
//        return LocalDate.parse(str, df);
//    }
//
//    Date getDateByLocal(LocalDate date) {
//
//        return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
//    }
//
//    Date getDateByLocal(LocalDateTime dateTime) {
//        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
//    }
//
//    public static void main(String[] args) throws RunnerException {
//        final Options opts = new OptionsBuilder()
//                .include(JmhTestApp.class.getSimpleName())
//                .forks(1)
//                //预热设置
//                .warmupIterations(2)
//                .warmupTime(TimeValue.microseconds(60))
//                //度量摄者
//                .measurementIterations(2)
//                .measurementTime(TimeValue.microseconds(30))
//                .build();
//        new Runner(opts).run();
//    }
//
//    @Benchmark
//    @Test
//    public void testParse() {
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        DateTime dateTime = new DateTime("2020-09-03", sdf);
//        Date date = dateTime.toJdkDate();
//        System.out.println(date);
//
//
//    }
//
//
//    @Benchmark
//    @Test
//    public void testParseLocalDate() {
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 构建DateTime对象
//        LocalDate parse = LocalDate.parse("2020-09-23", df);
//        Date date = getDateByLocal(parse);
//        System.out.println(date);
//
//    }
//
//    @Benchmark
//    @Test
//    public void testParseLocalDateTime() {
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        // 构建DateTime对象
//        LocalDateTime parse = LocalDateTime.parse("2020-09-23 00:00:00", df);
//        Date date = getDateByLocal(parse);
//        System.out.println(date);
//
//    }
//
//    @Benchmark
//    @Test
//    public void toStr() {
//        Date date = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd mm:HH:ss");
//        String format = sdf.format(date);
//        System.out.println(format);
//    }
//
//    @Benchmark
//    @Test
//    public void toStrLocal() {
//        LocalDateTime now = LocalDateTime.now();
//
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd mm:HH:ss");
//        String format = now.format(df);
//        System.out.println(format);
//    }
//
//    @Test
//    @Benchmark
//    public void offset() {
//
//        Date dateParse = DateKit.parse("2020-09-23 09:23:23");
//
//        System.out.println(DateKit.offset(dateParse, DateField.YEAR, -1));
//
//        System.out.println(DateKit.offset(dateParse, DateField.MONTH, -1));
//
//
//        System.out.println(DateKit.offset(dateParse, DateField.DAY_OF_YEAR, -1));
//
//
//        System.out.println(DateKit.offset(dateParse, DateField.HOUR, -1));
//
//
//        System.out.println(DateKit.offset(dateParse, DateField.MINUTE, -1));
//
//
//        System.out.println(DateKit.offset(dateParse, DateField.SECOND, -1));
//
//
//        System.out.println(DateKit.offset(dateParse, DateField.MILLISECOND, 1000));
//
//
//    }
//
//
//    @Test
//    @Benchmark
//    public void offsetLocal() {
//        // 获取指定日期偏移指定时间后的时间
//
//        LocalDateTime local = getLocal("2020-09-23 09:23:23");
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.YEARS))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.MONTHS))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.DAYS))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.HOURS))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.MINUTES))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(-1, ChronoUnit.SECONDS))));
//
//
//        System.out.println(DateTime.of(getDateByLocal(local.plus(1000, ChronoUnit.MILLIS))));
//
//
//    }
//
//    @Test
//    @Benchmark
//    public void testBetween() {
//        // 判断两个日期相差的时长，只保留绝对值
//        Date startDate = DateKit.parse("2020-09-30");
//        Date endDate = DateKit.parse("2020-09-01");
//        System.out.println(DateKit.between(startDate, endDate, DateUnit.DAY));
//        System.out.println(DateKit.between(startDate, endDate, DateUnit.WEEK, false));
//    }
//
//    @Test
//    @Benchmark
//    public void testBetweenLocal() {
//        // 判断两个日期相差的时长，只保留绝对值
//        LocalDate startDate = getLocalDate("2020-09-30");
//        LocalDate endDate = getLocalDate("2020-09-01");
//        System.out.println(startDate.toEpochDay() - endDate.toEpochDay());
//        System.out.println((startDate.toEpochDay() - endDate.toEpochDay()) / 7);
//    }
//
//    @Test
//    @Benchmark
//    public void testBetweenLocal2() {
//        // 判断两个日期相差的时长，只保留绝对值
//        LocalDateTime startDate = getLocal("2020-09-30 00:00:00");
//        LocalDateTime endDate = getLocal("2020-09-01 00:00:00");
//        Duration between = Duration.between(startDate, endDate);
//        System.out.println(between.toDays());
//        System.out.println(between.toDays() / 7);
//    }


}
