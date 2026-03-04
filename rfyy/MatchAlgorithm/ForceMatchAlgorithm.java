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
import com.current.rfyy.utils.ForceMatchUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 11:52
 * @Description: TODO
 **/
@Component("forceMatchAlgorithm")
@RequiredArgsConstructor
public class ForceMatchAlgorithm implements MatchAlgorithm {

    private final AlgorithmHelper algorithmHelper;


    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.FORCE;
    }

    @Override
    public boolean match(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgds, StrategyEnum strategy) {
        if (filterCgds.size() <= RfyyConstant.MAX_CANDIDATE_SIZE) {
            List<Cgd> result =
                    ForceMatchUtils.findFirstMatchByAmount(fp.getJshjBd(), filterCgds);

            if (!CollectionUtils.isEmpty(result)
                    && DateMatchUtils.getDateDifference(result, LocalDate.parse(fp.getKprq().substring(0, 10))) <= RfyyConstant.MAX_DATE_GAP) {
                MatchUtils.processMatchSuccess(
                        fp, result, xsf, ctx,
                        strategy + getType().getAlgorithm(),
                        MatchStatus.AUTO_MATCHED
                );
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matchWithWc(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgds, StrategyEnum strategy) {
        if (filterCgds.size() <= RfyyConstant.MAX_CANDIDATE_SIZE) {
            List<Cgd> forceMatched = ForceMatchUtils.findFirstMatchByAmountWithWc(fp.getJshjBd(), filterCgds, RfyyConstant.AMOUNT_DIFF);
            if (!CollectionUtils.isEmpty(forceMatched)
                    && DateMatchUtils.getDateDifference(forceMatched, LocalDate.parse(fp.getKprq().substring(0, 10))) <= RfyyConstant.MAX_DATE_GAP) {
                BigDecimal diff =
                        CalcUtils.compareJeWithWc(fp.getJshjBd(),
                                CalcUtils.calcCgdJe(forceMatched));

                if (diff == null) {
                    return false;
                }
                List<Cgd> result =
                        algorithmHelper.matchTail(fp, xsf, ctx,
                                forceMatched, diff, remainingCgds,
                                strategy + getType().getAlgorithm() + "_WC");
                if (result != null) {
                    return true;
                } else if (strategy.equals(StrategyEnum.SPMC_PH)) {
                    MatchUtils.processMatchWithWc(fp,
                            forceMatched,
                            xsf,
                            ctx,
                            strategy.getstrategy() + getType().getAlgorithm() + "_NO_TBD",
                            MatchStatus.AUTO_MATCHED,
                            diff.toPlainString());
                }
            }
        }
        return false;
    }
}
