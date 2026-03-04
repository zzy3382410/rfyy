package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.DataFilterUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/1/12 17:11
 * @Description: TODO 一对多 SPMC
 **/
@Slf4j
@Component
public class MatchBySpmcStrategy implements MatchStrategy {

    private final Map<String, MatchAlgorithm> algorithmMap;

    public MatchBySpmcStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }


    @Override
    public StrategyEnum getType() {
        return StrategyEnum.SPMC;
    }

    /**
     * 3.商品&金额&数量 一致
     * 根据商品名称过滤出一样的采购单据，然后比较合计金额和数量是否相等
     *
     */
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
                algorithmMap.get(MatchAlgorithmEnum.EACH.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.TOTAL.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.RQ_GROUP.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.YZYF.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.FORCE.getBeanName())
        );

        // 遍历 remainingFp（注意：用 Iterator，安全 remove）
        Iterator<Fp> iterator = remainingFps.iterator();
        while (iterator.hasNext()) {
            Fp fp = iterator.next();
            if (fp.isMatched()) {
                continue;
            }
            if (StringUtils.isEmpty(fp.getHandledSpmc())) {
                continue;
            }
            // 商品名称处理
            List<Cgd> matchedCgds = DataFilterUtils.filterCgdsBySpmc(fp, remainingCgds, xsf);
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
                // 尾差匹配
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
