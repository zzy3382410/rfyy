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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @Author: zzy
 * @Date: 2026/1/14 14:23
 * @Description: TODO PH_JE_SL
 **/
public class MatchByPhAndJeAndSl implements MatchStrategy {

    /**
     * 4
     * 匹配逻辑：
     * 1. 批号匹配 + 批号备注包含批号
     * 2. 金额一致
     * 3. 数量一致
     */
    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        List<Fp> remainingFps = xsf.getRemainingFp();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        Set<String> allPhSet = xsf.getPhs();
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
            if (!MatchUtils.isValid(fp.getCheckSpmc(), fp.getBz())) {
                continue;
            }
            // 匹配批号
            Set<String> matchedPhs = MatchUtils.matchPhBycgdPhsAndFpbz(allPhSet, fp.getBz());
            // 根据spmc和ph找到对应的采购单
            List<Cgd> matchedCgds = remainingCgds.stream()
                    .filter(cgd -> cgd.getCgdMxList().stream().allMatch(cgdMx -> {
                        // 提取明细匹配逻辑
                        String cgdSpmc = MatchUtils.handleSpmc(cgdMx.getSpmc());
                        if (StringUtils.isEmpty(cgdSpmc)) {
                            return false;
                        }
                        // 匹配逻辑： 批号匹配 + 备注包含批号
                        return matchedPhs.contains(cgdMx.getPh())
                                && fp.getBz().contains(cgdMx.getPh());
                    }))
                    .toList();
            if (!CollectionUtils.isEmpty(matchedCgds)) {
                BigDecimal cgdJeTotal = MatchUtils.calcCgdJe(matchedCgds);
                int cgdSlTotal = MatchUtils.calcCgdSl(matchedCgds);
                if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), cgdJeTotal)
                        && fp.getSpsl() == cgdSlTotal) {
                    matched = true;
                    MatchUtils.processMatchSuccess(fp,
                            matchedCgds,
                            xsf,
                            ctx,
                            StrategyEnum.PH_JE_SL,
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
