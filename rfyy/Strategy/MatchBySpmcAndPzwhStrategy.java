package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.DataFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/10 10:25
 * @Description: TODO  PZWH_JE_SL
 **/
@Component
public class MatchBySpmcAndPzwhStrategy implements MatchStrategy {

    private final Map<String, MatchAlgorithm> algorithmMap;

    public MatchBySpmcAndPzwhStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }



    @Override
    public StrategyEnum getType() {
        return null;
    }

    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        List<Fp> remainingFps = xsf.getRemainingFp();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        // 无数据可处理，直接短路
        if (xsf.allMatched()) {
            return true;
        }
        // 算法链
        List<MatchAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.TOTAL.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.RQ_GROUP.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.FORCE.getBeanName())
        );
        // 遍历 remainingFp
        Iterator<Fp> fpIterator = remainingFps.iterator();
        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();
            if (fp.isMatched()) {
                continue;
            }
            boolean matched = false;
            if (StringUtils.isEmpty(fp.getHandledSpmc())) {
                continue;
            }
            // 根据spmc找到对应的采购单
            List<Cgd> matchedCgds = DataFilterUtils.filterCgdsBySpmc(fp, remainingCgds);
            // 按pzwh分组
            Map<String, List<Cgd>> matchedPzwhCgds = matchedCgds.stream()
                    .collect(Collectors.groupingBy(Cgd::getPzwh));

            for (Map.Entry<String, List<Cgd>> entry : matchedPzwhCgds.entrySet()) {
                List<Cgd> cgdList = entry.getValue();
                for (MatchAlgorithm algorithm : chain) {
                    if (algorithm.match(fp, xsf, ctx, cgdList, remainingCgds, getType())) {
                        matched = true;
                        break;
                    }
                }
                // BigDecimal rqCgdJeTotal = CalcUtils.calcCgdJe(cgdList);
                // if (CalcUtils.compareJe(fp.getJshjBd(), rqCgdJeTotal)) {
                //     MatchUtils.processMatchSuccess(fp, cgdList, xsf, ctx, StrategyEnum.SPMC_PZWH, MatchStatus.AUTO_MATCHED);
                //     matched = true;
                //     break;
                // } else {
                //     // todo force match
                //     List<Cgd> forceMatchedCgds = ForceMatchUtils.findFirstMatchByAmount(fp.getJshjBd(), cgdList, RfyyConstant.NO_WC_MATCH);
                //     if (!forceMatchedCgds.isEmpty()) {
                //         MatchUtils.processMatchSuccess(
                //                 fp,
                //                 forceMatchedCgds,
                //                 xsf,
                //                 ctx,
                //                 getType() + "_FORCE",
                //                 MatchStatus.AUTO_MATCHED);
                //         matched = true;
                //         break;
                //     }
                // }
            }
            if (matched) {
                fpIterator.remove();
            }
        }
        return xsf.allMatched();
    }
}
