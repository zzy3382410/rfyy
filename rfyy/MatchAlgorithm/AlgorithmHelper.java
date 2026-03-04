package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.FpMx;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/12 11:28
 * @Description: TODO
 **/
@Service
public class AlgorithmHelper {

    /**
     * 逐条匹配
     *
     * @param fpjshj     发票金额
     * @param spsl       数量
     * @param kprq       开票日期
     * @param filterCgds 待匹配的采购单
     * @param wc         是否允许尾差
     * @return 匹配结果
     */
    public Cgd eachMatch(BigDecimal fpjshj, Integer spsl, String kprq, List<Cgd> filterCgds, String wc) {
        // 1️⃣ 单条
        List<Cgd> exact = filterCgds.stream()
                .filter(c -> {
                            boolean sl = spsl == c.getCgsl();
                            if ("1".equals(wc)) {
                                BigDecimal wcje = CalcUtils.compareJeWithWc(fpjshj, c.getHjjeBd());
                                return wcje != null && sl;

                            } else {
                                return CalcUtils.compareJe(fpjshj, c.getHjjeBd())
                                        && sl;
                            }
                        }
                ).collect(Collectors.toList());

        if (!exact.isEmpty()) {
            return DateMatchUtils.findClosestCgd(exact, kprq);
        }
        return null;
    }


    /**
     * 尾差匹配
     *
     * @param fp            发票
     * @param phs           批号
     * @param remainingCgds 采购单
     * @param wc            是否允许尾差
     * @return 匹配结果
     */
    public List<Cgd> yzyfMatch(Fp fp,
                               Set<String> phs,
                               List<Cgd> remainingCgds,
                               String wc) {
        if ("1".equals(fp.getSfyzyf())) {
            List<Cgd> finalMatchedCgds = new ArrayList<>();
            FpMx posFpmx = MatchUtils.getPositiveFpmx(fp.getFpmxList());
            FpMx negFpmx = MatchUtils.getNegativeFpmx(fp.getFpmxList());
            List<Cgd> matchedSpmcPhCgds = DataFilterUtils.filterCgdsBySpmcAndPh(fp, remainingCgds, fp.getPhs(), xsf);
            List<Cgd> matchedNegSpmcCgds = DataFilterUtils.filterNegCgdsBySpmc(fp, remainingCgds, xsf);
            // 分别匹配正负明细
            List<Cgd> posCgdList = this.yzyfMatchFpmx(posFpmx, matchedSpmcPhCgds, wc);
            List<Cgd> negCgdList = this.yzyfMatchFpmx(negFpmx, matchedNegSpmcCgds, wc);

            if (!CollectionUtils.isEmpty(posCgdList) && !CollectionUtils.isEmpty(negCgdList)) {
                // 正负金额和发票匹配
                if (CalcUtils.compareJe(CalcUtils.calcCgdJe(posCgdList).add(CalcUtils.calcCgdJe(negCgdList)), fp.getJshjBd())) {
                    finalMatchedCgds.addAll(posCgdList);
                    finalMatchedCgds.addAll(negCgdList);
                    return finalMatchedCgds;
                }

            }
        }
        return Collections.emptyList();
    }

    public List<Cgd> totalMatch(BigDecimal fpjshj, List<Cgd> filterCgds, String wc) {
        if (RfyyConstant.WC_MATCH.equals(wc)) {
            if (CalcUtils.compareJeWithWc(fpjshj, CalcUtils.calcCgdJe(filterCgds)) != null) {
                return filterCgds;
            }
        } else {
            if (CalcUtils.compareJe(fpjshj, CalcUtils.calcCgdJe(filterCgds))) {
                return filterCgds;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 匹配正负明细
     *
     * @param fpmx       发票明细
     * @param filterCgds 待匹配的采购单
     * @param wc         是否允许尾差
     * @return 匹配中采购单
     */
    public List<Cgd> yzyfMatchFpmx(FpMx fpmx, List<Cgd> filterCgds, String wc) {
        // 先逐个比较
        Cgd eachMatch = eachMatch(fpmx.getJshjBd(), MatchUtils.safeParseInt(fpmx.getSl()), fpmx.getKprq(), filterCgds, wc);
        if (eachMatch != null) {
            return List.of(eachMatch);
        }

        // 合计比较
        List<Cgd> totalMatch = totalMatch(fpmx.getJshjBd(), filterCgds, wc);
        if (!totalMatch.isEmpty()) {
            return filterCgds;
        } else {
            // 按日期groupby在比较金额
            Map<String, List<Cgd>> matchedRqCgds = filterCgds.stream().collect(Collectors.groupingBy(Cgd::getRq));
            // 可能存在多个匹配结果
            List<String> matchedRqs = new ArrayList<>();
            for (Map.Entry<String, List<Cgd>> entry : matchedRqCgds.entrySet()) {
                List<Cgd> cgdList = entry.getValue();
                BigDecimal rqCgdJeTotal = CalcUtils.calcCgdJe(cgdList);
                // todo rf 金额&数量必须一致  （原逻辑只要金额一致） && cgdSlTotal == fp.getSpsl()
                if (CalcUtils.compareJe(fpmx.getJshjBd(), rqCgdJeTotal, wc)) {
                    // 收集命中结果
                    matchedRqs.add(entry.getKey());
                }
            }
            if (!matchedRqs.isEmpty()) {
                String rq = DateMatchUtils.findClosestRq(matchedRqs, fpmx.getKprq());
                return matchedRqCgds.get(rq);
            }
        }
        // 暴力匹配
        if (filterCgds.size() <= RfyyConstant.MAX_CANDIDATE_SIZE) {
            return ForceMatchUtils.findFirstMatchByAmount(fpmx.getJshjBd(), filterCgds, wc);
        }

        return null;
    }


    /**
     * 尾差匹配
     *
     * @param fp       发票
     * @param xsf      销售方
     * @param ctx      匹配上下文
     * @param baseCgds 基础采购单
     * @param diff     尾差
     * @param allCgds  所有采购单
     * @param strategy 策略
     * @return 匹配结果
     */
    public List<Cgd> matchTail(Fp fp,
                               XsfMatchData xsf,
                               MatchContext ctx,
                               List<Cgd> baseCgds,
                               BigDecimal diff,
                               List<Cgd> allCgds,
                               String strategy) {

        Set<String> spggs = MatchUtils.extractSpggs(baseCgds);

        List<Cgd> tailPool =
                WcMatchHelper.extractTailDiffCgdsBySpmcAndGgxh(
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

        MatchUtils.processMatchSuccess(
                fp, finalList, xsf, ctx,
                strategy,
                MatchStatus.AUTO_MATCHED
        );

        return finalList;
    }

}
