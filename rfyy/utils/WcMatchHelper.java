package com.current.rfyy.utils;

import com.current.common.utils.StringUtils;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/11 9:53
 * @Description: TODO  尾差匹配辅助类
 **/
public class WcMatchHelper {

    /**
     * 提取正常采购单
     */
    public static List<Cgd> extractNormalCgds(List<Cgd> cgds) {
        return cgds.stream()
                .filter(cgd -> !WcMatchHelper.isTailDiffCgd(cgd))
                .collect(Collectors.toList());
    }


    /**
     * 是否尾差采购单
     */
    public static boolean isTailDiffCgd(Cgd cgd) {
        // 无批号
        boolean ph = cgd.getCgdMxList().stream().anyMatch(mx ->
                StringUtils.isBlank(mx.getPh()) && "采购退补价执行".equals(mx.getZy()));
        // 小金额
        BigDecimal je = cgd.getHjjeBd().abs();
        return je.compareTo(RfyyConstant.AMOUNT_DIFF) <= 0 && ph;
    }

    /**
     * 是否尾差采购单(大金额)
     */
    public static boolean isTbCgd(Cgd cgd) {
        // 无批号
        boolean ph = cgd.getCgdMxList().stream().anyMatch(mx ->
                StringUtils.isBlank(mx.getPh()) && "采购退补价执行".equals(mx.getZy()));
        return cgd.getHjjeBd().compareTo(BigDecimal.ZERO) <= 0 && ph;
    }

    /**
     * 提取尾差采购单
     */
    public static List<Cgd> extractTailDiffCgdsBySpmc(List<Cgd> cgds, Fp fp) {
        return cgds.stream()
                .filter(WcMatchHelper::isTailDiffCgd)
                .filter(cgd -> cgd.getCgdMxList().stream().anyMatch(mx ->
                {
                    // 匹配逻辑：名称包含关系
                    return MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc());
                }))
                .collect(Collectors.toList());
    }

    /**
     * 通过spmc和ggxh提取尾差采购单 (小金额)
     *
     */
    public static List<Cgd> extractTailDiffCgdsBySpmcAndGgxh(List<Cgd> cgds, Fp fp) {
        return cgds.stream()
                .filter(WcMatchHelper::isTailDiffCgd)
                .filter(cgd -> cgd.getCgdMxList().stream().anyMatch(mx ->
                {
                    // 匹配逻辑：名称包含关系
                    return MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                            && MatchUtils.matchGgxh(fp.getGgxh(), fp.getHandledGgxh(), mx.getSpgg(), mx.getHandledSpgg())
                            && DateMatchUtils.getDateDifference(mx.getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP;
                }))
                .collect(Collectors.toList());
    }

    /**
     * 通过spmc和ggxh提取尾差采购单 (大金额)
     *
     */
    public static List<Cgd> extractTbCgdsBySpmcAndGgxh(List<Cgd> cgds, Fp fp) {
        return cgds.stream()
                .filter(WcMatchHelper::isTbCgd)
                .filter(cgd -> cgd.getCgdMxList().stream().anyMatch(mx ->
                {
                    // 匹配逻辑：名称包含关系
                    return MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                            && MatchUtils.matchGgxh(fp.getGgxh(),fp.getHandledGgxh(), mx.getSpgg(),mx.getHandledSpgg())
                            && DateMatchUtils.getDateDifference(mx.getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP;
                }))
                .collect(Collectors.toList());
    }


    /**
     * 计算尾差合计金额
     */
    public static BigDecimal calcTailDiffTotal(List<Cgd> tailDiffCgds) {
        return tailDiffCgds.stream()
                .map(Cgd::getHjjeBd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 是否【主金额 + 尾差】可以匹配
     */
    public static boolean matchWithTail(BigDecimal fpJe,
                                        BigDecimal mainCgdJe,
                                        BigDecimal tailDiffJe) {
        return CalcUtils.compareJe(fpJe, mainCgdJe.add(tailDiffJe));
    }

    /**
     * 寻找【主金额 + 尾差】单条
     */
    public static Cgd findSingleTailDiff(
            BigDecimal targetDiff,
            List<Cgd> tailDiffCgds) {

        if (targetDiff == null || tailDiffCgds.isEmpty()) {
            return null;
        }

        for (Cgd cgd : tailDiffCgds) {
            BigDecimal je = cgd.getHjjeBd();
            if (CalcUtils.compareJe(targetDiff, je)) {
                return cgd;
            }
        }
        return null;
    }


    /**
     * 寻找【主金额 + 尾差】两条
     */
    public static List<Cgd> findTailDiffCombination(
            BigDecimal targetDiff,
            List<Cgd> tailDiffCgds,
            String targetDateStr) {

        if (targetDiff == null || tailDiffCgds == null || tailDiffCgds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate targetDate = LocalDate.parse(targetDateStr);

        List<List<Cgd>> matchedCombinations = new ArrayList<>();

        // 1️⃣ 单条匹配
        for (Cgd cgd : tailDiffCgds) {
            BigDecimal je = cgd.getHjjeBd();
            if (CalcUtils.compareJe(targetDiff, je)) {
                matchedCombinations.add(List.of(cgd));
            }
        }

        // 2️⃣ 两条组合匹配
        for (int i = 0; i < tailDiffCgds.size(); i++) {
            for (int j = i + 1; j < tailDiffCgds.size(); j++) {

                BigDecimal sum = tailDiffCgds.get(i).getHjjeBd()
                        .add(tailDiffCgds.get(j).getHjjeBd());

                if (CalcUtils.compareJe(targetDiff, sum)) {
                    matchedCombinations.add(
                            List.of(tailDiffCgds.get(i), tailDiffCgds.get(j))
                    );
                }
            }
        }

        if (matchedCombinations.isEmpty()) {
            return Collections.emptyList();
        }

        // 3️⃣ 选择日期最近的组合
        return matchedCombinations.stream()
                .min(Comparator.comparing(combo -> DateMatchUtils.getDateDifference(combo, targetDate)))
                .orElse(Collections.emptyList());
    }

    /**
     * 退补单匹配
     *
     * @param fp       发票
     * @param baseCgds 基础采购单
     * @param diff     尾差
     * @param allCgds  所有采购单
     * @return 匹配结果
     */
    public static List<Cgd> matchTbCgd(Fp fp,
                                       List<Cgd> baseCgds,
                                       BigDecimal diff,
                                       List<Cgd> allCgds
    ) {

        List<Cgd> tailPool =
                WcMatchHelper.extractTbCgdsBySpmcAndGgxh(
                        allCgds, fp);

        if (CollectionUtils.isEmpty(tailPool)) {
            return null;
        }

        List<Cgd> tail =
                WcMatchHelper.findTailDiffCombination(diff, tailPool, baseCgds.get(0).getRq());

        if (CollectionUtils.isEmpty(tail)) {
            return null;
        }

        List<Cgd> finalList = new ArrayList<>(baseCgds);
        finalList.addAll(tail);
        return finalList;
    }

}
