package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.CalcUtils;
import com.current.rfyy.utils.DateMatchUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/12 11:43
 * @Description: TODO rq匹配算法
 **/

@Component("rqGroupMatchAlgorithm")
@RequiredArgsConstructor
public class RqGroupMatchAlgorithm implements MatchAlgorithm {

    private final AlgorithmHelper algorithmHelper;

    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.RQ_GROUP;
    }

    @Override
    public boolean match(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgds, StrategyEnum strategy) {
        Map<String, List<Cgd>> rqGroup =
                filterCgds.stream().collect(Collectors.groupingBy(Cgd::getRq));
        // boolean yzyf = "1".equals(fp.getSfyzyf());
        List<String> matchedRq = new ArrayList<>();
        for (Map.Entry<String, List<Cgd>> entry : rqGroup.entrySet()) {
            if (CalcUtils.compareJe(fp.getJshjBd(), CalcUtils.calcCgdJe(entry.getValue()))
                    && DateMatchUtils.getDateDifference(entry.getValue().get(0).getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP) {
                // if (!yzyf) {
                matchedRq.add(entry.getKey());
                // } else {
                // List<Cgd> yzyfMatch = algorithmHelper.yzyfMatch(fp, xsf.getPhs(), remainingCgds, RfyyConstant.NO_WC_MATCH);
                // if (!yzyfMatch.isEmpty()) {
                //     MatchUtils.processMatchSuccess(fp,
                //             yzyfMatch,
                //             xsf,
                //             ctx,
                //             strategy + ALGORITHM + MatchAlgorithmEnum.YZYF.getAlgorithm(),
                //             MatchStatus.AUTO_MATCHED);
                //     return true;
                // }
                // }
            }
        }
        // 取日期相隔最近的结果
        if (!matchedRq.isEmpty()) {
            String closestRq = DateMatchUtils.findClosestRq(matchedRq, fp.getKprq());
            List<Cgd> matchedCgds = rqGroup.get(closestRq);
            MatchUtils.processMatchSuccess(
                    fp, matchedCgds, xsf, ctx,
                    strategy + getType().getAlgorithm(),
                    MatchStatus.AUTO_MATCHED
            );
            return true;
        }

        // for (List<Cgd> group : rqGroup.values()) {
        //     if (MatchUtils.compareJe(fp.getJshjBd(), MatchUtils.calcCgdJe(group))
        //             && MatchUtils.getDateDifference(group.get(0).getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP) {
        //         MatchUtils.processMatchSuccess(
        //                 fp, group, xsf, ctx,
        //                 strategy + ALGORITHM,
        //                 MatchStatus.AUTO_MATCHED
        //         );
        //         return true;
        //     } else if ("1".equals(fp.getSfyzyf())) {
        //         List<Cgd> yzyfMatch = algorithmHelper.yzyfMatch(fp, xsf.getPhs(), remainingCgds, RfyyConstant.NO_WC_MATCH);
        //         if (!yzyfMatch.isEmpty()) {
        //             MatchUtils.processMatchSuccess(fp,
        //                     yzyfMatch,
        //                     xsf,
        //                     ctx,
        //                     strategy + ALGORITHM + MatchAlgorithmEnum.YZYF.getAlgorithm(),
        //                     MatchStatus.AUTO_MATCHED);
        //             return true;
        //         }
        //     }
        // }
        return false;
    }

    @Override
    public boolean matchWithWc(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgds, StrategyEnum strategy) {
        Map<String, List<Cgd>> rqGroup =
                filterCgds.stream().collect(Collectors.groupingBy(Cgd::getRq));

        for (List<Cgd> group : rqGroup.values()) {

            BigDecimal diff =
                    CalcUtils.compareJeWithWc(fp.getJshjBd(),
                            CalcUtils.calcCgdJe(group));

            if (diff != null) {
                List<Cgd> result =
                        algorithmHelper.matchTail(fp, xsf, ctx,
                                group, diff, remainingCgds,
                                strategy + getType().getAlgorithm() + "_WC");
                if (result != null) {
                    return true;
                } else if (strategy.equals(StrategyEnum.SPMC_PH)) {
                    MatchUtils.processMatchWithWc(fp,
                            group,
                            xsf,
                            ctx,
                            strategy.getstrategy() + getType().getAlgorithm() + "_NO_TBD",
                            MatchStatus.AUTO_MATCHED,
                            diff.toPlainString());
                }
            }

            // // 一正一负有尾差互补
            // if ("1".equals(fp.getSfyzyf())) {
            //     List<Cgd> yzyfMatch = algorithmHelper.yzyfMatch(fp, xsf.getPhs(), remainingCgds, RfyyConstant.WC_MATCH);
            //     if (!yzyfMatch.isEmpty()) {
            //         MatchUtils.processMatchSuccess(fp,
            //                 yzyfMatch,
            //                 xsf,
            //                 ctx,
            //                 strategy + ALGORITHM + MatchAlgorithmEnum.YZYF.getAlgorithm() + "_WC",
            //                 MatchStatus.AUTO_MATCHED);
            //         return true;
            //     }
            // }

        }
        return false;
    }
}
