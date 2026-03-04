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
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 9:58
 * @Description: TODO 逐条匹配
 **/
@Component("eachMatchAlgorithm")
@RequiredArgsConstructor
public class EachMatchAlgorithm implements MatchAlgorithm {

    private final AlgorithmHelper algorithmHelper;


    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.EACH;
    }

    @Override
    public boolean match(Fp fp,
                         XsfMatchData xsf,
                         MatchContext ctx,
                         List<Cgd> filterCgds,
                         List<Cgd> remainingCgd,
                         StrategyEnum strategy) {
        Cgd result = algorithmHelper.eachMatch(fp.getJshjBd(), fp.getSpsl(), fp.getKprq(), filterCgds, RfyyConstant.NO_WC_MATCH);
        if (result != null) {
            MatchUtils.processMatchSuccess(
                    fp, List.of(result), xsf, ctx,
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
        // 1️⃣ 单条 + 尾差
        for (Cgd exact : filterCgds) {

            if (fp.getSpsl() != exact.getCgsl()) {
                continue;
            }

            BigDecimal diff =
                    CalcUtils.compareJeWithWc(fp.getJshjBd(),
                            exact.getHjjeBd());

            if (diff == null) {
                continue;
            }

            List<Cgd> result =
                    algorithmHelper.matchTail(fp, xsf, ctx,
                            List.of(exact), diff, remainingCgd,
                            strategy + getType().getAlgorithm() + "_WC");

            if (result != null) {
                return true;
            } else if (strategy.equals(StrategyEnum.SPMC_PH)) {
                MatchUtils.processMatchWithWc(fp,
                        List.of(exact),
                        xsf,
                        ctx,
                        strategy.getstrategy() + getType().getAlgorithm() + "_NO_TBD",
                        MatchStatus.AUTO_MATCHED,
                        diff.toPlainString());
            }
        }
        return false;
    }
}
