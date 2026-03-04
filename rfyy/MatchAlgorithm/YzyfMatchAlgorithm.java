package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 11:50
 * @Description: TODO 一正一负匹配算法
 **/
@Component("yzyfMatchAlgorithm")
@RequiredArgsConstructor
public class YzyfMatchAlgorithm implements MatchAlgorithm {

    private final AlgorithmHelper algorithmHelper;

    private static final String ALGORITHM = MatchAlgorithmEnum.YZYF.getAlgorithm();

    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.YZYF;
    }

    @Override
    public boolean match(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgds, StrategyEnum strategy) {
        List<Cgd> yzyfMatch = algorithmHelper.yzyfMatch(fp, xsf.getPhs(), remainingCgds, RfyyConstant.NO_WC_MATCH);
        if (!yzyfMatch.isEmpty()) {
            MatchUtils.processMatchSuccess(fp,
                    yzyfMatch,
                    xsf,
                    ctx,
                    strategy + ALGORITHM,
                    MatchStatus.AUTO_MATCHED);
            return true;
        }
        return false;
    }

    @Override
    public boolean matchWithWc(Fp fp, XsfMatchData xsf, MatchContext ctx, List<Cgd> filterCgds, List<Cgd> remainingCgd, StrategyEnum strategy) {
        List<Cgd> yzyfMatch = algorithmHelper.yzyfMatch(fp, xsf.getPhs(), remainingCgd, RfyyConstant.WC_MATCH);
        if (!yzyfMatch.isEmpty()) {
            MatchUtils.processMatchSuccess(fp,
                    yzyfMatch,
                    xsf,
                    ctx,
                    strategy + ALGORITHM + "_WC",
                    MatchStatus.AUTO_MATCHED);
            return true;
        }
        return false;
    }
}
