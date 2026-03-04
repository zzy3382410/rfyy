package com.current.rfyy.utils;

import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.CgdMx;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: zzy
 * @Date: 2026/2/26 15:03
 * @Description: TODO 数据过滤工具类
 **/
public class DataFilterUtils {

    /**
     * 根据商品名过滤采购单（for 循环，避免 stream 性能损耗）
     */
    public static List<Cgd> filterCgdsBySpmc(Fp fp, List<Cgd> cgds) {
        return filterCgdsBySpmc(fp, cgds, null);
    }

    /**
     * 根据商品名过滤采购单（优先使用 xfs 预建索引）
     */
    public static List<Cgd> filterCgdsBySpmc(Fp fp, List<Cgd> cgds, XsfMatchData xsf) {
        List<Cgd> source = candidateBySpmcIndex(fp, cgds, xsf);
        List<Cgd> result = new ArrayList<>();

        for (Cgd cgd : source) {
            boolean ok = true;
            for (CgdMx mx : cgd.getCgdMxList()) {
                if (!MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result.add(cgd);
            }
        }
        return result;
    }

    /**
     * 根据商品名过滤发票
     */
    public static List<Fp> filterFpsBySpmc(Cgd cgd, List<Fp> fps) {
        List<Fp> result = new ArrayList<>();
        List<CgdMx> cgdMxList = cgd.getCgdMxList();
        for (Fp fp : fps) {
            boolean match = cgdMxList.stream()
                    .anyMatch(mx ->
                            MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc()));
            if (match) {
                result.add(fp);
            }
        }
        return result;
    }

    /**
     * 根据商品名过滤发票
     */
    public static List<Fp> filterFpsBySpmcAndGgxh(Cgd cgd, List<Fp> fps, boolean sfTbCgd) {
        List<Fp> result = new ArrayList<>();
        List<CgdMx> cgdMxList = cgd.getCgdMxList();
        for (Fp fp : fps) {
            boolean tbFp = true;
            if (sfTbCgd) {
                tbFp = fp.getFpmxList().stream().anyMatch(mx -> mx.getJshjBd().compareTo(BigDecimal.ZERO) <= 0);
            }
            boolean match = cgdMxList.stream().anyMatch(mx -> {
                return MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                        && MatchUtils.matchGgxh(mx.getSpgg(), mx.getHandledSpgg(), fp.getGgxh(), fp.getHandledGgxh());
            });
            if (match && tbFp) {
                result.add(fp);
            }
        }
        return result;
    }


    /**
     * 根据商品名过滤负数金额采购单（for 循环，避免 stream 性能损耗）
     */
    public static List<Cgd> filterNegCgdsBySpmc(Fp fp, List<Cgd> cgds) {
        return filterNegCgdsBySpmc(fp, cgds, null);
    }

    public static List<Cgd> filterNegCgdsBySpmc(Fp fp, List<Cgd> cgds, XsfMatchData xsf) {
        List<Cgd> source = candidateBySpmcIndex(fp, cgds, xsf);
        List<Cgd> result = new ArrayList<>();

        for (Cgd cgd : source) {
            boolean ok = true;
            for (CgdMx mx : cgd.getCgdMxList()) {
                if (!MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                        || mx.getHsje().compareTo(BigDecimal.ZERO) > 0) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result.add(cgd);
            }
        }
        return result;
    }

    /**
     * 根据商品名和批号过滤采购单（for 循环，避免 stream 性能损耗）
     */
    public static List<Cgd> filterCgdsBySpmcAndPh(Fp fp, List<Cgd> cgds, Set<String> fpphSet) {
        return filterCgdsBySpmcAndPh(fp, cgds, fpphSet, null);
    }

    public static List<Cgd> filterCgdsBySpmcAndPh(Fp fp, List<Cgd> cgds, Set<String> fpphSet, XsfMatchData xsf) {
        List<Cgd> source = candidateBySpmcAndPhIndex(fp, cgds, fpphSet, xsf);
        List<Cgd> result = new ArrayList<>();
        for (Cgd cgd : source) {
            boolean ok = true;
            for (CgdMx mx : cgd.getCgdMxList()) {
                // 匹配逻辑：名称包含关系 + 批号匹配 + 备注包含批号
                if (!fpphSet.contains(mx.getPh())
                        || !MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                    // || !fp.getBz().contains(mx.getPh())
                ) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result.add(cgd);
            }
        }
        return result;
    }

    /**
     * 根据商品名和批号过滤发票
     */
    public static List<Fp> filterFpsBySpmcAndPh(Cgd cgd, List<Fp> fps) {
        List<Fp> result = new ArrayList<>();
        List<CgdMx> cgdMxList = cgd.getCgdMxList();
        for (Fp fp : fps) {
            if (StringUtils.isEmpty(fp.getBz())) {
                continue;
            }
            boolean match = cgdMxList.stream().anyMatch(mx ->
                            StringUtils.isNotEmpty(mx.getPh())
                                    && fp.getPhs().contains(mx.getPh())
                                    && MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                    // && fp.getBz().contains(mx.getPh())
            );
            if (match) {
                result.add(fp);
            }
        }
        return result;
    }

    /**
     * 根据商品名过滤采购单 并且没有批号的
     */
    public static List<Cgd> filterCgdsBySpmcAndNoPh(Fp fp, List<Cgd> cgds) {
        return filterCgdsBySpmcAndNoPh(fp, cgds, null);
    }

    public static List<Cgd> filterCgdsBySpmcAndNoPh(Fp fp, List<Cgd> cgds, XsfMatchData xsf) {
        List<Cgd> source = candidateBySpmcIndex(fp, cgds, xsf);
        List<Cgd> result = new ArrayList<>();
        for (Cgd cgd : source) {
            boolean ok = true;
            for (CgdMx mx : cgd.getCgdMxList()) {
                if (StringUtils.isNotBlank(mx.getPh())) {
                    ok = false;
                    break;
                }
                if (!MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result.add(cgd);
            }
        }
        return result;
    }


    /**
     * 根据商品名和批号获取正数采购单
     */
    public static List<Cgd> filterPosCgdsBySpmcAndPh(Fp fp, List<Cgd> cgds, Set<String> fpphSet) {
        return filterPosCgdsBySpmcAndPh(fp, cgds, fpphSet, null);
    }

    public static List<Cgd> filterPosCgdsBySpmcAndPh(Fp fp, List<Cgd> cgds, Set<String> fpphSet, XsfMatchData xsf) {
        List<Cgd> source = candidateBySpmcAndPhIndex(fp, cgds, fpphSet, xsf);
        List<Cgd> result = new ArrayList<>();
        for (Cgd cgd : source) {
            boolean ok = true;
            for (CgdMx mx : cgd.getCgdMxList()) {
                boolean tailDiffCgd = WcMatchHelper.isTailDiffCgd(cgd);
                // 匹配逻辑：名称包含关系 + 批号匹配 + 备注包含批号
                if (!MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), mx.getHandledSpmc())
                        || !fpphSet.contains(mx.getPh())
                        || StringUtils.isBlank(mx.getPh())
                        // || !fp.getBz().contains(mx.getPh())
                        || tailDiffCgd) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result.add(cgd);
            }
        }
        return result;
    }

    private static List<Cgd> candidateBySpmcAndPhIndex(Fp fp, List<Cgd> cgds, Set<String> fpphSet, XsfMatchData xsf) {
        if (xsf == null || fpphSet == null || fpphSet.isEmpty()) {
            return cgds;
        }

        Set<Cgd> phCandidates = new LinkedHashSet<>();
        for (String ph : fpphSet) {
            phCandidates.addAll(xsf.getCgdByPh(ph));
        }
        if (phCandidates.isEmpty()) {
            return List.of();
        }

        Set<Cgd> spmcCandidates = new LinkedHashSet<>();
        for (String token : splitTokens(fp.getHandledSpmc())) {
            spmcCandidates.addAll(xsf.getCgdByHandledSpmcFuzzy(token));
        }
        for (String token : splitTokens(fp.getSpmc())) {
            spmcCandidates.addAll(xsf.getCgdBySpmcFuzzy(token));
        }

        if (!spmcCandidates.isEmpty()) {
            phCandidates.retainAll(spmcCandidates);
        }

        if (cgds == null || cgds.isEmpty()) {
            return new ArrayList<>(phCandidates);
        }
        if (xsf.isRemainingScope(cgds)) {
            return new ArrayList<>(phCandidates);
        }

        Set<Cgd> scopeSet = new HashSet<>(cgds);
        phCandidates.retainAll(scopeSet);
        return new ArrayList<>(phCandidates);
    }

    private static List<Cgd> candidateBySpmcIndex(Fp fp, List<Cgd> cgds, XsfMatchData xsf) {
        if (xsf == null) {
            return cgds;
        }

        Set<Cgd> candidates = new LinkedHashSet<>();
        for (String token : splitTokens(fp.getHandledSpmc())) {
            candidates.addAll(xsf.getCgdByHandledSpmcFuzzy(token));
        }
        for (String token : splitTokens(fp.getSpmc())) {
            candidates.addAll(xsf.getCgdBySpmcFuzzy(token));
        }
        if (candidates.isEmpty()) {
            return cgds;
        }

        if (cgds == null || cgds.isEmpty()) {
            return new ArrayList<>(candidates);
        }
        if (xsf.isRemainingScope(cgds)) {
            return new ArrayList<>(candidates);
        }
        Set<Cgd> scopeSet = new HashSet<>(cgds);
        candidates.retainAll(scopeSet);
        return new ArrayList<>(candidates);
    }

    private static List<String> splitTokens(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        String[] split = value.split("_");
        List<String> tokens = new ArrayList<>(split.length);
        for (String item : split) {
            if (StringUtils.isNotBlank(item)) {
                tokens.add(item.trim());
            }
        }
        return tokens;
    }
}
