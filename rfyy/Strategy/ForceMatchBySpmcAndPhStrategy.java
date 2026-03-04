package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.DataFilterUtils;
import com.current.rfyy.utils.MatchUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/2/25 10:43
 * @Description: TODO  FORCE_MATCH_SPMC_PH
 **/
@Component
public class ForceMatchBySpmcAndPhStrategy implements MatchStrategy {

    private final Map<String, MatchAlgorithm> algorithmMap;

    public ForceMatchBySpmcAndPhStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.FORCE_MATCH_SPMC_PH;
    }

    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        if (xsf.allMatched()) {
            return true;
        }
        // 算法链
        List<MatchAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.FORCE.getBeanName())
        );
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        Iterator<Fp> iterator = xsf.getRemainingFp().iterator();

        while (iterator.hasNext()) {
            Fp fp = iterator.next();

            if (fp.isMatched()
                    || !MatchUtils.isValid(fp.getHandledSpmc(), fp.getBz())) {
                continue;
            }

            // 只过滤一次
            List<Cgd> matchedCgds =
                    DataFilterUtils.filterCgdsBySpmcAndPh(fp, remainingCgds, fp.getPhs(), xsf);

            if (CollectionUtils.isEmpty(matchedCgds)) {
                continue;
            }
            boolean matched = false;
            for (MatchAlgorithm algorithm : chain) {
                // 精确匹配
                if (algorithm.match(fp, xsf, ctx, matchedCgds, remainingCgds, getType())) {
                    matched = true;
                    break;
                }
                if (algorithm.matchWithWc(fp, xsf, ctx, matchedCgds, remainingCgds, getType())) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                iterator.remove();
            }
        }
        return xsf.allMatched();
    }

}
