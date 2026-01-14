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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/9 10:22
 * @Description: TODO SPMC_PH_JE_SL
 **/
@Slf4j
public class MatchBySpmcAndPhAndJeAndSl implements MatchStrategy {

    /**
     * 1.商品名称&批号&金额&数量 一致
     * 根据商品名称即批号过滤出一样的采购单据，然后比较合计金额和数量是否相等
     *
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
            // 商品名称处理
            String spmc = fp.getCheckSpmc();
            String oriSpmc = fp.getOriSpmc();
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
                        // 匹配逻辑：名称包含关系 + 批号匹配 + 备注包含批号
                        return (spmc.contains(cgdSpmc) || cgdSpmc.contains(spmc) || oriSpmc.contains(cgdSpmc) || cgdSpmc.contains(oriSpmc))
                                && matchedPhs.contains(cgdMx.getPh())
                                && fp.getBz().contains(cgdMx.getPh());
                    }))
                    .toList();
            List<Cgd> finalMatchedCgds = new ArrayList<>();
            // 先逐个比较
            for (Cgd cgd : matchedCgds) {
                if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), new BigDecimal(cgd.getCgdHjje()))
                        && fp.getSpsl() == cgd.getCgsl()) {
                    finalMatchedCgds.add(cgd);
                }
            }
            if (CollectionUtils.isEmpty(finalMatchedCgds)) {
                // 合计比较
                BigDecimal cgdJeTotal = MatchUtils.calcCgdJe(matchedCgds);
                if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), cgdJeTotal)) {
                    // 处理命中
                    MatchUtils.processMatchSuccess(fp, matchedCgds, xsf, ctx, StrategyEnum.SPMC_PH_JE_SL, MatchStatus.AUTO_MATCHED);
                    matched = true;
                } else {
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
            } else {
                // 去日期最近的
                Cgd cgd = MatchUtils.findClosestCgd(finalMatchedCgds, fp.getKprq());
                MatchUtils.processMatchSuccess(fp, new ArrayList<>(List.of(cgd)), xsf, ctx, StrategyEnum.SPMC_PH_JE_SL, MatchStatus.AUTO_MATCHED);
                matched = true;
            }

            // // 计算采购单的数量 & 金额
            // int cgdSlTotal = MatchUtils.calcCgdSl(matchedCgdMxs);
            // BigDecimal cgdJeTotal = MatchUtils.calcCgdJe(matchedCgdMxs);
            // // todo rf 金额&数量必须一致  （原逻辑只要金额一致） && cgdSlTotal == fp.getSpsl()
            // if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), cgdJeTotal)) {
            //     // 处理命中
            //     MatchUtils.processMatchSuccess(fp, matchedCgdMxs, xsf, ctx, StrategyEnum.SPMC_PH_JE_SL, MatchStatus.AUTO_MATCHED);
            //     matched = true;
            // } else {
            //     // 按日期groupby在比较金额
            //     Map<String, List<CgdMx>> matchedRqCgdMxs = matchedCgdMxs.stream().collect(Collectors.groupingBy(CgdMx::getRq));
            //     // 可能存在多个匹配结果
            //     List<String> matchedRqs = new ArrayList();
            //     for (Map.Entry<String, List<CgdMx>> entry : matchedRqCgdMxs.entrySet()) {
            //         List<CgdMx> cgdMxList = entry.getValue();
            //         int rqCgdSlTotal = MatchUtils.calcCgdSl(cgdMxList);
            //         BigDecimal rqCgdJeTotal = MatchUtils.calcCgdJe(cgdMxList);
            //         // todo rf 金额&数量必须一致  （原逻辑只要金额一致） && cgdSlTotal == fp.getSpsl()
            //         if (MatchUtils.compareJe(new BigDecimal(fp.getJshj()), rqCgdJeTotal)) {
            //             // 收集命中结果
            //             matchedRqs.add(entry.getKey());
            //         }
            //     }
            //     // 取日期最近的一组
            //     if (!matchedRqs.isEmpty()) {
            //         String rq = MatchUtils.findClosestRq(matchedRqs, fp.getKprq());
            //         List<CgdMx> cgdMxList = matchedRqCgdMxs.get(rq);
            //         MatchUtils.processMatchSuccess(fp, cgdMxList, xsf, ctx, StrategyEnum.SPMC_PH_JE_SL, MatchStatus.AUTO_MATCHED);
            //         matched = true;
            //     }
            // }
            if (matched) {
                fpIterator.remove();
            }
        }
        return xsf.allMatched();
    }

}
