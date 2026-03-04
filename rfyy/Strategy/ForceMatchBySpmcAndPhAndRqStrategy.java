package com.current.rfyy.Strategy;

import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.DataFilterUtils;
import com.current.rfyy.utils.DateMatchUtils;
import com.current.rfyy.utils.ForceMatchUtils;
import com.current.rfyy.utils.MatchUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/2/25 10:43
 * @Description: TODO  FORCE_MATCH_SPMC_PH_RQ
 **/
@Component
public class ForceMatchBySpmcAndPhAndRqStrategy implements MatchStrategy {

    private final Map<String, MatchAlgorithm> algorithmMap;

    public ForceMatchBySpmcAndPhAndRqStrategy(Map<String, MatchAlgorithm> algorithmMap) {
        this.algorithmMap = algorithmMap;
    }

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.FORCE_MATCH_SPMC_PH_RQ;
    }

    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        if (xsf.allMatched()) {
            return true;
        }
        // 算法链
        List<MatchAlgorithm> chain = List.of(
                algorithmMap.get(MatchAlgorithmEnum.FORCE.getBeanName())
        );
        List<Cgd> remainingCgds = xsf.getRemainingCgd();
        Iterator<Fp> iterator = xsf.getRemainingFp().iterator();

        while (iterator.hasNext()) {
            Fp fp = iterator.next();

            if (fp.isMatched()
                    || !MatchUtils.isValid(fp.getHandledSpmc(), fp.getBz())) {
                continue;
            }

            // 只过滤一次
            List<Cgd> filterCgds =
                    DataFilterUtils.filterCgdsBySpmcAndPh(fp, remainingCgds, fp.getPhs(), xsf);

            if (CollectionUtils.isEmpty(filterCgds)) {
                continue;
            }
            boolean matched = false;
            // todo 按日期分组进行暴力匹配
            List<Cgd> matchedCgds = ForceMatchUtils.findMatchByRqGroup(fp.getJshjBd(), filterCgds, BigDecimal.ZERO);
            if (!CollectionUtils.isEmpty(matchedCgds)
                    && DateMatchUtils.getDateDifference(matchedCgds, LocalDate.parse(fp.getKprq().substring(0, 10))) <= RfyyConstant.MAX_DATE_GAP) {
                MatchUtils.processMatchSuccess(
                        fp,
                        matchedCgds,
                        xsf,
                        ctx,
                        getType(),
                        MatchStatus.AUTO_MATCHED);
                matched = true;
            }
            if (matched) {
                iterator.remove();
            }
        }
        return xsf.allMatched();
    }

}
