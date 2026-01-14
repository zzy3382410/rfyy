package com.current.rfyy.Strategy;

import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.MatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/12 16:24
 * @Description: TODO 一对一 SPMC_JE_SL_RQ
 **/
@Slf4j
public class MatchBySpmcAndJeAndSlAndRq implements MatchStrategy {

    /**
     * 2.商品名称&金额&数量 一致
     * 根据商品名称和合计金额和数量是否相等匹配
     * 多条则取日期最近的
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
                        // && fp.getSpsl() == cgdMx.getCgsl()
                        return (spmc.contains(cgdSpmc) || cgdSpmc.contains(spmc) || oriSpmc.contains(cgdSpmc) || cgdSpmc.contains(oriSpmc))
                                && MatchUtils.compareJe(new BigDecimal(fp.getJshj()), cgdMx.getHsje())
                                ;
                    }))
                    .sorted(Comparator.comparing(Cgd::getRq))
                    .toList();
            if (!CollectionUtils.isEmpty(matchedCgds)) {
                // 取日期最接近的
                Cgd matchedCgd = MatchUtils.findClosestCgd(matchedCgds, fp.getKprq());
                if (MatchUtils.getDateDifference(matchedCgd.getRq(), fp.getKprq()) > 60) {
                    continue;
                } else {
                    // 命中
                    matched = true;
                    MatchUtils.processMatchSuccess(fp,
                            new ArrayList<>(List.of(matchedCgd)),
                            xsf,
                            ctx,
                            StrategyEnum.SPMC_JE_SL_RQ,
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
