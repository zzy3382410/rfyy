package com.current.rfyy.utils;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.CgdMx;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/9 11:47
 * @Description: TODO
 **/
@Slf4j
public class MatchUtils {


    /**
     * 校验商品名称和规格型号是否有效
     *
     * @param spmc 商品名称
     * @param ph   规格型号
     * @return 是否有效
     */
    public static boolean isValid(String spmc, String ph) {
        return spmc != null && !spmc.isBlank()
                && ph != null && !ph.isBlank();
    }

    /**
     * 根据采购单批号规则，从发票备注中匹配出所有可能的批号
     * 根据找到公司所有的批号，来反向看发票的备注中是否存在批号，如若有则取出。
     * 因很多的发票备注中不规范，不知道是否是批号所以反向归集来找
     *
     * @param cgdPhs 采购单批号集合
     * @param fpbz   发票备注
     * @return 匹配到的批号数组
     */
    public static Set<String> matchPhBycgdPhsAndFpbz(Set<String> cgdPhs, String fpbz) {
        Set<String> matches = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(fpbz)) {
            // 正则匹配字母数字和特殊符号组合
            Matcher matcher = Pattern.compile("[\\w&+-]+").matcher(fpbz);
            while (matcher.find()) {
                String candidate = matcher.group();
                if (cgdPhs.contains(candidate)) matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * 处理商品名称字段
     * 1.提取星号(*)后的部分作为商品名称
     * 2.去除剂量规格（如25mg）
     * 3.去掉括号及其后面的内容（无论是否有星号）
     * 4.将字符串标准化并转换为半角字符
     *
     * @param spmc 原始商品名称字符串
     * @return 处理后的商品名称字符串
     */
    public static String handleSpmc(String spmc) {
        if (StringUtils.isEmpty(spmc)) {
            return null;
        }
        // 定义正则表达式模式
        String regex = "\\*([^*]*)$";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建匹配器--->匹配货物名称
        Matcher matcher = pattern.matcher(spmc);
        // 默认用原字符串处理
        String processed = spmc;
        if (matcher.find()) {
            // 商品名称,在调用 matcher.group(1) 之前，必须确保 matcher.find() 返回 true;提取星号后的部分
            processed = matcher.group(1);
        }
        // TODO zzy 新增：去除剂量规格（如25mg）
        String removedDose = processed.replaceAll("\\d+[a-zA-Z]+", "");
        // 去掉括号及其后面的内容（无论是否有星号）
        //(PP)50%葡萄糖注射液 遇到这种就有问题，括号在前面的商品名称
        // 安全分割逻辑
        String[] parts = removedDose.split("[()]");
        String splitStr = parts.length > 0 ? parts[0].trim() : "";
        // 将字符串标准化并转换为半角字符
        return Normalizer.normalize(splitStr, Normalizer.Form.NFKC);
    }

    /**
     * 计算采购单明细金额总和
     *
     * @param cgdList 采购单明细列表
     * @return 采购单金额总和
     */
    public static BigDecimal calcCgdJe(List<Cgd> cgdList) {
        if (cgdList == null) {
            return BigDecimal.ZERO;
        }

        return cgdList.stream()
                .map(cgd -> cgd != null ? cgd.getCgdHjje() : null)
                .filter(Objects::nonNull)
                .map(str -> {
                    try {
                        return new BigDecimal(str);
                    } catch (NumberFormatException e) {
                        log.warn("金额格式错误: {}", str);
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算采购单明细数量总和
     *
     * @param cgdList 采购单明细列表
     * @return 采购单数量总和
     */
    public static int calcCgdSl(List<Cgd> cgdList) {
        return cgdList.stream().mapToInt(Cgd::getCgsl).sum();
    }


    /**
     * 计算采购单明细数量总和
     *
     * @param cgdMxList 采购单明细列表
     * @return 采购单数量总和
     */
    public static int calcCgdMxSl(List<CgdMx> cgdMxList) {
        return cgdMxList.stream().mapToInt(CgdMx::getCgsl).sum();
    }

    /**
     * 计算采购单明细金额总和
     *
     * @param cgdMxList 采购单明细列表
     * @return 采购单金额总和
     */
    public static BigDecimal calcCgdMxJe(List<CgdMx> cgdMxList) {
        return cgdMxList.stream().map(CgdMx::getHsje).reduce(BigDecimal.ZERO, BigDecimal::add);

    }


    /**
     * 比较发票金额和采购单金额是否相等，允许误差
     *
     * @param fpje  发票金额
     * @param cgdje 采购单金额
     * @return 是否相等
     */
    public static boolean compareJe(BigDecimal fpje, BigDecimal cgdje) {
        return (cgdje.subtract(fpje).abs().compareTo(new BigDecimal(RfyyConstant.AMOUNT_THRESHOLD)) <= 0);
    }


    /**
     * 统一处理匹配成功逻辑
     * （发票 ↔ 采购单）
     * 一对多 或 一对一
     * @param fp          发票
     * @param matchedCgds 命中的采购单列表
     * @param xsf         发票匹配数据
     * @param ctx         匹配上下文
     * @param strategy    匹配策略
     * @param matchStatus 匹配状态
     */
    public static void processMatchSuccess(Fp fp, List<Cgd> matchedCgds,
                                           XsfMatchData xsf,
                                           MatchContext ctx,
                                           StrategyEnum strategy,
                                           String matchStatus) {
        if (fp.isMatched()) {
            return;
        }
        if (matchedCgds == null || matchedCgds.isEmpty()) {
            return;
        }
        String fpInfo = String.format("%s_%s%s%s", fp.getXfmc(), fp.getFphm(), fp.getFpdm(), fp.getSdphm());

        Set<String> matchedDjbhs = matchedCgds.stream()
                .map(Cgd::getDjbh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 标记采购单
        Iterator<Cgd> cgdIterator = xsf.getRemainingCgd().iterator();
        while (cgdIterator.hasNext()) {
            Cgd cgd = cgdIterator.next();
            if (matchedDjbhs.contains(cgd.getDjbh())) {
                log.info("匹配成功 - 发票: {}, 采购单: {}", fpInfo, cgd.getDjbh());

                cgd.markMatched(MatchStatus.AUTO_MATCHED);
                ctx.addMatched(fp, cgd, strategy.getstrategy(), matchStatus);

                // 从剩余池中移除，防止被后续策略或循环重复处理
                cgdIterator.remove();
            }
        }
        // 标记发票
        fp.markMatched(matchStatus);
    }

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
            LocalDateTime targetDateTime = LocalDateTime.parse(kprq, DATE_TIME_FORMATTER);

            String closestRq = null;
            long minSecondsDiff = Long.MAX_VALUE;

            // 2. 遍历日期列表进行比对
            for (String rqStr : rqList) {
                // 解析列表中的日期（仅日期）并转为当天0点
                LocalDate currentDate = LocalDate.parse(rqStr, DATE_ONLY_FORMATTER);
                LocalDateTime currentDateTime = currentDate.atStartOfDay();

                // 3. 计算绝对时间差（秒）
                long diff = Math.abs(Duration.between(targetDateTime, currentDateTime).getSeconds());

                // 4. 更新最小值
                if (diff < minSecondsDiff) {
                    minSecondsDiff = diff;
                    closestRq = rqStr;
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

            if (diff < minDiff) {
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
}
