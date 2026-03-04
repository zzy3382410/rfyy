package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.CalcUtils;
import com.current.rfyy.utils.DateMatchUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 9:58
 * @Description: TODO 合计匹配
 **/
@Component("totalMatchAlgorithm")
@RequiredArgsConstructor
public class TotalMatchAlgorithm implements MatchAlgorithm {

    private final AlgorithmHelper algorithmHelper;

    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.TOTAL;
    }

    @Override
    public boolean match(Fp fp,
                         XsfMatchData xsf,
                         MatchContext ctx,
                         List<Cgd> filterCgds,
                         List<Cgd> remainingCgd,
                         StrategyEnum strategy) {
        if (CalcUtils.compareJe(fp.getJshjBd(), CalcUtils.calcCgdJe(filterCgds))
                && filterCgds.stream().allMatch(cgd ->
                DateMatchUtils.getDateDifference(cgd.getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP)) {
            MatchUtils.processMatchSuccess(
                    fp, filterCgds, xsf, ctx,
                    strategy + getType().getAlgorithm(),
                    MatchStatus.AUTO_MATCHED);
            return true;
        }
        return false;
    }

    @Override
    public boolean matchWithWc(Fp fp,
                               XsfMatchData xsf,
                               MatchContext ctx,
                               List<Cgd> filterCgds,
                               List<Cgd> remainingCgd,
                               StrategyEnum strategy) {
        BigDecimal totalJe = CalcUtils.calcCgdJe(filterCgds);
        BigDecimal totalDiff =
                CalcUtils.compareJeWithWc(fp.getJshjBd(), totalJe);

        if (totalDiff != null
                && filterCgds.stream().allMatch(cgd ->
                DateMatchUtils.getDateDifference(cgd.getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP)) {

            List<Cgd> result =
                    algorithmHelper.matchTail(fp, xsf, ctx,
                            filterCgds, totalDiff, remainingCgd,
                            strategy + getType().getAlgorithm() + "_WC");

            if (result != null) {
                return true;
            } else if (strategy.equals(StrategyEnum.SPMC_PH)) {
                MatchUtils.processMatchWithWc(fp,
                        filterCgds,
                        xsf,
                        ctx,
                        strategy.getstrategy() + getType().getAlgorithm() + "_NO_TBD",
                        MatchStatus.AUTO_MATCHED,
                        totalDiff.toPlainString());
            }
        }
        return false;
    }
}
