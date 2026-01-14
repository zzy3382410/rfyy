package com.current.rfyy.Strategy;

import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.MatchUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/12 17:11
 * @Description: TODO 一对多 SPMC_JE_SL
 **/
public class MatchBySpmcAndJeAndSl implements MatchStrategy {

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
        // 遍历 remainingFp（注意：用 Iterator，安全 remove）
        Iterator<Fp> fpIterator = remainingFps.iterator();
        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();
            if (fp.isMatched()) {
                continue;
            }
            boolean matched = false;
            if (StringUtils.isEmpty(fp.getCheckSpmc())) {
                continue;
            }
            // 商品名称处理
            String spmc = fp.getCheckSpmc();
            String oriSpmc = fp.getOriSpmc();
            List<Cgd> matchedCgds = remainingCgds.stream()
                    .filter(cgd -> cgd.getCgdMxList().stream().allMatch(cgdMx -> {
                        // 提取明细匹配逻辑
                        String cgdSpmc = MatchUtils.handleSpmc(cgdMx.getSpmc());
                        if (StringUtils.isEmpty(cgdSpmc)) {
                            return false;
                        }
                        // 匹配逻辑：名称包含关系 + 金额
                        return (spmc.contains(cgdSpmc) || cgdSpmc.contains(spmc) || oriSpmc.contains(cgdSpmc) || cgdSpmc.contains(oriSpmc));
                    }))
                    .sorted(Comparator.comparing(Cgd::getRq))
                    .toList();
            if (!CollectionUtils.isEmpty(matchedCgds)) {
                BigDecimal cgdJeTotal = MatchUtils.calcCgdJe(matchedCgds);
                int cgdSlTotal = MatchUtils.calcCgdSl(matchedCgds);
                // && fp.getSpsl() == cgdSlTotal
                if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), cgdJeTotal)
                ) {
                    matched = true;
                    MatchUtils.processMatchSuccess(fp,
                            matchedCgds,
                            xsf,
                            ctx,
                            StrategyEnum.SPMC_JE_SL,
                            MatchStatus.AUTO_MATCHED);
                }
            }
            if (matched) {
                fpIterator.remove();
            }

        }
        return xsf.allMatched();
    }
}
