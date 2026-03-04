package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchFpAlgorithm;
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
 * @Date: 2026/02/26 17:11
 * @Description: TODO cgd一对多fp  CGD_SPMC
 **/
@Slf4j
@Component
public class MatchFpBySpmcStrategy implements MatchStrategy {

    private final Map<String, MatchFpAlgorithm> algorithmMap;

    public MatchFpBySpmcStrategy(Map<String, MatchFpAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }


    @Override
    public StrategyEnum getType() {
        return StrategyEnum.CGD_SPMC;
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
        List<MatchFpAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.TOTAL_FP.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.RQ_GROUP_FP.getBeanName()),
                algorithmMap.get(MatchAlgorithmEnum.FORCE_FP.getBeanName())
        );
        // 遍历
        Iterator<Cgd> iterator = remainingCgds.iterator();
        while (iterator.hasNext()) {
            Cgd cgd = iterator.next();
            if (cgd.isMatched()) {
                continue;
            }
            boolean tbCgd = MatchUtils.isTbCgd(cgd);
            List<Fp> matchedFps = DataFilterUtils.filterFpsBySpmcAndGgxh(cgd, remainingFps, tbCgd);
            if (CollectionUtils.isEmpty(matchedFps)) {
                continue;
            }
            boolean matched = false;
            for (MatchFpAlgorithm algorithm : chain) {
                // 精确匹配
                if (algorithm.matchFp(cgd, xsf, ctx, matchedFps, getType())) {
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
