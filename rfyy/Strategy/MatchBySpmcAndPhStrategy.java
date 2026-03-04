package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.DataFilterUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/1/9 10:22
 * @Description: TODO SPMC_PH
 **/
@Slf4j
@Component
public class MatchBySpmcAndPhStrategy implements MatchStrategy {

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.SPMC_PH;
    }

    private final Map<String, MatchAlgorithm> algorithmMap;

    public MatchBySpmcAndPhStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }


    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {

        if (xsf.allMatched()) {
            return true;
        }

        // 算法链
        List<MatchAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.EACH.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.TOTAL.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.RQ_GROUP.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.YZYF.getBeanName())
                // algorithmMap.get(MatchAlgorithmEnum.FORCE.getBeanName())
        );

        List<Fp> remainingFps = xsf.getRemainingFp();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();

        Iterator<Fp> iterator = remainingFps.iterator();

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
                // // 尾差匹配 （暴力带尾差放在下一步）
                // if (MatchAlgorithmEnum.FORCE.equals(algorithm.getType())){
                //     continue;
                // }
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
