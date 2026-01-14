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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/14 14:39
 * @Description: TODO PH_RQ_JE_SL
 **/
public class MatchByPhAndRqAndJeAndSl implements MatchStrategy{

    /**
     * 5
     * 匹配逻辑：
     * 1. 匹配spmc、批号、金额、数量
     * 2. 匹配日期
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
                // 按日期groupby在比较金额
                Map<String, List<Cgd>> matchedRqCgds = matchedCgds.stream().collect(Collectors.groupingBy(Cgd::getRq));
                // 可能存在多个匹配结果
                List<String> matchedRqs = new ArrayList();
                for (Map.Entry<String, List<Cgd>> entry : matchedRqCgds.entrySet()) {
                    List<Cgd> cgdList = entry.getValue();
                    BigDecimal rqCgdJeTotal = MatchUtils.calcCgdJe(cgdList);
                    // todo rf 金额&数量必须一致  （原逻辑只要金额一致） && cgdSlTotal == fp.getSpsl()
                    if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), rqCgdJeTotal)) {
                        // 收集命中结果
                        matchedRqs.add(entry.getKey());
                    }
                    if (!matchedRqs.isEmpty()) {
                        String rq = MatchUtils.findClosestRq(matchedRqs, fp.getKprq());
                        List<Cgd> matchedRqCgdList = matchedRqCgds.get(rq);
                        MatchUtils.processMatchSuccess(fp, matchedRqCgdList, xsf, ctx, StrategyEnum.SPMC_PH_JE_SL, MatchStatus.AUTO_MATCHED);
                        matched = true;
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
