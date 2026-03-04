package com.current.rfyy.utils;

import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/26 15:09
 * @Description: TODO 日期匹配工具类
 **/
@Slf4j
public class DateMatchUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 查找最接近的日期字符串
     *
     * @param rqList 日期字符串列表 yyyy-MM-dd
     * @param kprq   目标日期字符串 yyyy-MM-dd HH:mm:ss
     * @return 最接近的日期字符串
     */
    public static String findClosestRq(List<String> rqList, String kprq) {
        if (rqList == null || rqList.isEmpty() || kprq == null || kprq.isEmpty()) {
            return null;
        }

        try {
            // 1. 解析目标日期（带时分秒）
            String kprqPart = kprq.length() > 10 ? kprq.substring(0, 10) : kprq;
            LocalDate targetDateTime = LocalDate.parse(kprqPart, DATE_ONLY_FORMATTER);

            String closestRq = null;
            long minDaysDiff = Long.MAX_VALUE;

            // 2. 遍历日期列表进行比对
            for (String rqStr : rqList) {
                if (rqStr == null || rqStr.isEmpty()) {
                    continue;
                }

                // 统一处理日期字符串，保留原始值用于返回
                String datePart = rqStr.length() > 10 ? rqStr.substring(0, 10) : rqStr;
                LocalDate currentDate = LocalDate.parse(datePart, DATE_ONLY_FORMATTER);

                // 3. 计算天数差（更直观）
                long diff = Math.abs(ChronoUnit.DAYS.between(targetDateTime, currentDate));

                // 4. 更新最小值
                if (diff < minDaysDiff) {
                    minDaysDiff = diff;
                    closestRq = rqStr; // 返回原始日期字符串
                }
            }

            return closestRq;

        } catch (Exception e) {
            // 处理解析异常，例如日期格式不正确
            log.error("日期解析失败: kprq={}, msg={}", kprq, e.getMessage());
            return rqList.get(0); // 发生异常时兜底返回列表第一个，或根据业务逻辑调整
        }
    }


    /**
     * 寻找最接近的采购单
     *
     * @param cgdList 采购单列表
     * @param kprq    目标日期字符串
     * @return 最接近的采购单
     */
    public static Cgd findClosestCgd(List<Cgd> cgdList, String kprq) {
        Cgd closestCgd = null;
        long minDiff = Long.MAX_VALUE;
        // 解析KPRQ为LocalDateTime
        LocalDateTime kprqDateTime = LocalDateTime.parse(kprq, DATE_TIME_FORMATTER);
        for (Cgd cgd : cgdList) {
            // 解析字符串日期为LocalDate
            LocalDate cgdDate = LocalDate.parse(cgd.getRq(), DATE_ONLY_FORMATTER);
            // 将LocalDate转换为LocalDateTime，时间部分设为00:00:00
            LocalDateTime recordDateTime = cgdDate.atStartOfDay();
            // 计算时间差
            long diff = Math.abs(ChronoUnit.SECONDS.between(recordDateTime, kprqDateTime));

            if (diff < minDiff && diff <= RfyyConstant.MAX_DATE_GAP * 24 * 60 * 60) {
                minDiff = diff;
                closestCgd = cgd;
            }
        }
        return closestCgd;
    }

    /**
     * 获取两个日期之间的天数差
     *
     * @param cgdRq 采购单日期 yyyy-MM-dd
     * @param kprq  发票日期 yyyy-MM-dd HH:mm:ss
     * @return 天数差
     */
    public static long getDateDifference(String cgdRq, String kprq) {
        // 将字符串转换为 LocalDate 对象
        LocalDate date1 = LocalDate.parse(cgdRq, DATE_ONLY_FORMATTER);
        LocalDate date2 = LocalDate.parse(kprq.substring(0, 10), DATE_ONLY_FORMATTER);
        // 计算两个日期的差值（天）
        return Math.abs(ChronoUnit.DAYS.between(date1, date2));
    }


    /**
     * 计算采购单组合与目标日期的“日期差”
     */
    public static long getDateDifference(List<Cgd> combo, LocalDate targetDate) {

        return combo.stream()
                .map(cgd -> LocalDate.parse(cgd.getRq().substring(0, 10), DATE_ONLY_FORMATTER))
                .mapToLong(date -> Math.abs(ChronoUnit.DAYS.between(date, targetDate)))
                .max()
                .orElse(Long.MAX_VALUE);
    }

    /**
     * 计算发牌组合与目标日期的“最小日期差”
     */
    public static long getFpDateDifference(List<Fp> combo, LocalDate targetDate) {

        return combo.stream()
                .map(fp -> LocalDate.parse(fp.getKprq().substring(0, 10)))
                .mapToLong(date -> Math.abs(ChronoUnit.DAYS.between(date, targetDate)))
                .max()
                .orElse(Long.MAX_VALUE);
    }
}
