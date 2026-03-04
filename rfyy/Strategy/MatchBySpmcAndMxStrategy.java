package com.current.rfyy.Strategy;

import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.CalcUtils;
import com.current.rfyy.utils.DateMatchUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/12 16:24
 * @Description: TODO 一对一 SPMC_MX
 **/
@Slf4j
@Component
public class MatchBySpmcAndMxStrategy implements MatchStrategy {

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.SPMC_MX;
    }

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
        // 遍历 remainingFp
        Iterator<Fp> fpIterator = remainingFps.iterator();
        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();
            if (fp.isMatched()) {
                continue;
            }
            boolean matched = false;
            if (StringUtils.isEmpty(fp.getHandledSpmc())) {
                continue;
            }
            List<Cgd> matchedCgds = remainingCgds.stream()
                    .filter(cgd -> {
                        // 采购单总金额匹配
                        if (!CalcUtils.compareJe(fp.getJshjBd(), cgd.getHjjeBd())) {
                            return false;
                        }
                        // 检查所有明细是否匹配
                        return cgd.getCgdMxList().stream().allMatch(cgdMx -> {
                            if (StringUtils.isEmpty(cgdMx.getHandledSpmc())) {
                                return false;
                            }
                            // 明细金额匹配
                            boolean mxjeMatched = MatchUtils.matchMxje(fp.getFpmxList(), cgd.getCgdMxList());
                            return MatchUtils.matchSpmc(fp.getHandledSpmc(), fp.getSpmc(), cgdMx.getHandledSpmc()) && mxjeMatched;
                        });
                    })
                    .sorted(Comparator.comparing(Cgd::getRq))
                    .toList();
            if (!CollectionUtils.isEmpty(matchedCgds)) {
                // 取日期最接近的
                Cgd matchedCgd = DateMatchUtils.findClosestCgd(matchedCgds, fp.getKprq());
                if (matchedCgd != null) {
                    // 命中
                    matched = true;
                    MatchUtils.processMatchSuccess(fp,
                            new ArrayList<>(List.of(matchedCgd)),
                            xsf,
                            ctx,
                            StrategyEnum.SPMC_MX,
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
