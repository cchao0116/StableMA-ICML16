/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 常用的日期处理工具
 * 
 * @author Hanke Chen
 * @version $Id: DateUtil.java, v 0.1 16 Sep 2013 21:01:38 chench Exp $
 */
public final class DateUtil {

    /** yyyyMMdd */
    public final static String SHORT_FORMAT           = "yyyyMMdd";

    /** yyyyMMddHHmmss */
    public final static String LONG_FORMAT            = "yyyyMMddHHmmss";

    /** yyyy-MM-dd */
    public final static String WEB_FORMAT             = "yyyy-MM-dd";

    /** HHmmss */
    public final static String TIME_FORMAT            = "HHmmss";

    /** yyyyMM */
    public final static String MONTH_FORMAT           = "yyyyMM";

    /** yyyy年MM月dd日 */
    public final static String CHINA_FORMAT           = "yyyy年MM月dd日";

    /** yyyy-MM-dd HH:mm:ss */
    public final static String LONG_WEB_FORMAT        = "yyyy-MM-dd HH:mm:ss";

    /** yyyy-MM-dd HH:mm */
    public final static String LONG_WEB_FORMAT_NO_SEC = "yyyy-MM-dd HH:mm";

    /**
     * 判断两个时间，是否是当日当时
     * 
     * @param l1
     * @param l2
     * @return
     */
    public static boolean sameDayAndHour(long l1, long l2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(l2);

        return (cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY))
               && (cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * Returns the value of the given calendar field.
     * 
     * @param l1    timeVal
     * @param type  calendar-specific value
     * @return
     */
    public static int get(long l1, int type) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);
        return cal1.get(type);
    }

    /**
     * 返回分钟数：<br/>
     * E.g., at 10:04:15.250 PM the MINUTE is 4.
     * 
     * @param l1
     * @return
     */
    public static int getMinOfHour(long l1) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);
        return cal1.get(Calendar.MINUTE);
    }

    /**
     * 24小时制小时
     * 
     * @param l1
     * @return
     */
    public static int getHourOfDay(long l1) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);
        return cal1.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 这一天是这一年的第几天
     * 
     * @param l1
     * @return
     */
    public static int getDayOfYear(long l1) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);
        return cal1.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 这一天是周几
     * 
     * @param l1
     * @return
     */
    public static int getDayOfWeek(long l1) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(l1);
        return cal1.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 日期字符串解析成日期对象基础方法，可以在此封装出多种便捷的方法直接使用
     * 
     * @param dateStr 日期字符串
     * @param format 输入的格式
     * @return 日期对象
     * @throws ParseException 
     */
    public static Date parse(String dateStr, String format) throws ParseException {
        if (StringUtil.isBlank(format)) {
            throw new ParseException("format can not be null.", 0);
        }

        if (StringUtil.isBlank(dateStr)) {
            throw new ParseException("date string's length is too small.", 0);
        }

        //调用API解析日期
        return new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE).parse(dateStr);
    }

    /**
     * 格式化当前时间
     * 
     * @param format 输出的格式
     * @return
     */
    public static String formatCurrent(String format) {
        if (StringUtil.isBlank(format)) {
            return StringUtil.EMPTY_STRING;
        }

        return format(new Date(), format);
    }

    /**
     * 日期对象解析成日期字符串基础方法，可以据此封装出多种便捷的方法直接使用
     * 
     * @param date 待格式化的日期对象
     * @param format 输出的格式
     * @return 格式化的字符串
     */
    public static String format(Date date, String format) {
        if (date == null || StringUtil.isBlank(format)) {
            return null;
        }

        return new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE).format(date);
    }

}
