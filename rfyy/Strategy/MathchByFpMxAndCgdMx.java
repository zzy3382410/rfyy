package com.current.rfyy.Strategy;

import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.*;
import com.current.rfyy.utils.MatchUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/14 15:15
 * @Description: TODO
 **/
public class MathchByFpMxAndCgdMx implements MatchStrategy {

    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {

        List<Fp> remainingFps = xsf.getRemainingFp();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        // 无数据可处理，直接短路
        if (xsf.allMatched()) {
            return true;
        }
        Iterator<Fp> fpIt = remainingFps.iterator();
        while (fpIt.hasNext()) {
            Fp fp = fpIt.next();
            String fpInfo = fp.getXfmc() + "_" +
                    fp.getFphm() + fp.getFpdm() + fp.getSdphm();

            List<CgdMx> allMatchedCgdMxs = new ArrayList<>();

            boolean allFpMxMatched = true;

            // 1️⃣ 逐笔发票明细
            for (FpMx fpMx : fp.getFpmxList()) {
                String mxSpmc = fpMx.getHwhyslwmc();
                String checkedMxSpmc = MatchUtils.handleSpmc(mxSpmc);
                int mxSl = Integer.parseInt(Optional.ofNullable(fpMx.getSl()).orElse("0"));

                // 2️⃣ 在剩余采购单中找匹配明细
                List<CgdMx> matchedCgdMxs = remainingCgds.stream()
                        .flatMap(cgd -> cgd.getCgdMxList().stream())
                        .filter(cgdMx -> {
                            String cgdSpmc = MatchUtils.handleSpmc(cgdMx.getSpmc());
                            if (StringUtils.isBlank(cgdSpmc)) {
                                return false;
                            }
                            boolean spmcMatch = checkedMxSpmc.contains(cgdSpmc)
                                    || cgdSpmc.contains(checkedMxSpmc)
                                    || mxSpmc.contains(cgdSpmc)
                                    || cgdSpmc.contains(mxSpmc);

                            boolean jeMatch = MatchUtils.compareJe(new BigDecimal(fpMx.getJshj()), cgdMx.getHsje());

                            boolean slMatch = mxSl == cgdMx.getCgsl();

                            return spmcMatch && jeMatch && slMatch;
                        })
                        .toList();

                if (matchedCgdMxs.isEmpty()) {
                    allFpMxMatched = false;
                    break;
                }
                allMatchedCgdMxs.addAll(matchedCgdMxs);
            }

            if (!allFpMxMatched) {
                continue;
            }

            List<CgdMx> finalMxs = allMatchedCgdMxs;
            // 对matchCgds去重
            allMatchedCgdMxs = allMatchedCgdMxs.stream().collect(Collectors.collectingAndThen(
                    Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(CgdMx::getId))), ArrayList::new));
            // 筛选的采购单明细数量和发票明细数量不相等取日期出现最多的
            if (allMatchedCgdMxs.size() != fp.getFpmxList().size()) {
                finalMxs = selectBestGroupByDate(allMatchedCgdMxs, fp.getKprq());
            }

            // 4️⃣ 倒算金额 & 数量
            int totalSl = finalMxs.stream().mapToInt(CgdMx::getCgsl).sum();
            BigDecimal totalJe = finalMxs.stream()
                    .map(CgdMx::getHsje)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean hit =
                    MatchUtils.compareJe(
                            new BigDecimal(fp.getJshj()), totalJe)
                            && fp.getSpsl() == totalSl;

            if (!hit) {
                continue;
            }
            // 根据命中明细找出命中的采购单
            List<CgdMx> finalMxs1 = finalMxs;
            List<Cgd> matchedCgds = remainingCgds.stream()
                    .filter(cgd -> finalMxs1.stream()
                            .anyMatch(cgdMx -> cgdMx.getDjbh().equals(cgd.getDjbh())))
                    .toList();

            // 5️⃣ 命中处理（统一）
            MatchUtils.processMatchSuccess(
                    fp,
                    matchedCgds,
                    xsf,
                    ctx,
                    StrategyEnum.FPMX_CGDMX_SPMC_JE_SL,
                    MatchStatus.AUTO_MATCHED
            );
            fpIt.remove();
        }

        return xsf.allMatched();
    }

    /**
     * 按日期选择采购单明细
     * 筛选出日期出现次数最多的采购单明细
     */
    private List<CgdMx> selectBestGroupByDate(List<CgdMx> matchCgds, String kprq) {

        // 统计每个日期的出现次数
        Map<String, Long> rqCountMap = matchCgds.stream()
                .map(CgdMx::getRq)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        // 找到最大出现次数
        long maxCount = rqCountMap.values().stream()
                .max(Long::compareTo)
                .orElse(0L);
        // 收集所有出现次数等于最大值的日期
        List<String> rqs = rqCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();
        // 找出离开票日期最近的
        String closestRq = MatchUtils.findClosestRq(rqs, kprq);
        return matchCgds.stream().filter(obj -> obj.getRq().equals(closestRq)).collect(Collectors.toList());

    }


}
