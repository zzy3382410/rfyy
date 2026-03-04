package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: zzy
 * @Date: 2026/1/9 10:22
 * @Description: TODO YZYF_SPMC_PH
 **/
@Slf4j
@Component
public class YzyfMatchBySpmcAndPhStrategy implements MatchStrategy {

    private final Map<String, MatchAlgorithm> algorithmMap;

    public YzyfMatchBySpmcAndPhStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.YZYF_SPMC_PH;
    }

    /**
     * 1.商品名称&批号&金额&数量 一致
     * 根据商品名称即批号过滤出一样的采购单据，然后比较合计金额和数量是否相等
     *
     */
    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        if (xsf.allMatched()) {
            return true;
        }
        List<Fp> remainingFps = xsf.getRemainingFp();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        Set<String> allPhSet = xsf.getPhs();
        // 无数据可处理，直接短路

        // 算法链
        List<MatchAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.YZYF.getBeanName())
        );
        // 遍历 remainingFp
        Iterator<Fp> fpIterator = remainingFps.iterator();
        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();
            if (fp.isMatched() || !"1".equals(fp.getSfyzyf())) {
                continue;
            }
            if (!MatchUtils.isValid(fp.getHandledSpmc(), fp.getBz())) {
                continue;
            }
            boolean matched = false;
            for (MatchAlgorithm algorithm : chain) {
                // 精确匹配
                if (algorithm.match(fp, xsf, ctx, remainingCgds, remainingCgds, getType())) {
                    matched = true;
                    break;
                }
                // 尾差匹配
                if (algorithm.matchWithWc(fp, xsf, ctx, remainingCgds, remainingCgds, getType())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // 正大天晴匹配逻辑
                List<Cgd> filterCgds = DataFilterUtils.filterPosCgdsBySpmcAndPh(fp, remainingCgds, allPhSet);
                BigDecimal wc = fp.getJshjBd().subtract(CalcUtils.calcCgdJe(filterCgds));
                // if (CalcUtils.compareSl(fp.getSpsl(), CalcUtils.calcCgdSl(filterCgds))) {
                List<Cgd> tailPool =
                        WcMatchHelper.extractTbCgdsBySpmcAndGgxh(
                                remainingCgds, fp);

                if (!CollectionUtils.isEmpty(tailPool)) {
                    // 汇总匹配再暴力
                    if (CalcUtils.compareJe(wc, CalcUtils.calcCgdJe(tailPool))) {
                        filterCgds.addAll(tailPool);
                        matched = true;
                    } else {
                        List<Cgd> negMatchedCgds = ForceMatchUtils.findFirstMatchByAmount(wc, tailPool, RfyyConstant.NO_WC_MATCH);
                        if (!negMatchedCgds.isEmpty()) {
                            filterCgds.addAll(negMatchedCgds);
                            matched = true;
                        }
                    }
                    if (matched) {
                        MatchUtils.processMatchSuccess(
                                fp,
                                filterCgds,
                                xsf,
                                ctx,
                                getType() + "_FORCE",
                                MatchStatus.AUTO_MATCHED);
                    }
                }
            }
            if (matched) {
                fpIterator.remove();
            }
        }
        return xsf.allMatched();
    }


}
