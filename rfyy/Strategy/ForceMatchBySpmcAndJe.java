package com.current.rfyy.Strategy;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/10 10:43
 * @Description: TODO  FORCE_MATCH_SPMC_JE
 **/
@Component
public class ForceMatchBySpmcAndJe implements MatchStrategy {

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.FORCE_MATCH_SPMC_JE;
    }

    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {
        if (xsf.allMatched()) {
            return true;
        }
        Iterator<Fp> fpIterator = xsf.getRemainingFp().iterator();

        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();

            if (fp.isMatched() || StringUtils.isBlank(fp.getHandledSpmc())) {
                continue;
            }

            // 按商品过滤
            List<Cgd> candidates = DataFilterUtils.filterCgdsBySpmc(fp, xsf.getRemainingCgd(), xsf);
            if (candidates.isEmpty() || candidates.size() > RfyyConstant.MAX_CANDIDATE_SIZE) {
                // 按商品和批号过滤
                candidates = DataFilterUtils.filterCgdsBySpmcAndPh(fp, xsf.getRemainingCgd(), fp.getPhs(), xsf);
                if (candidates.isEmpty() || candidates.size() > RfyyConstant.MAX_CANDIDATE_SIZE) {
                    continue;
                }
            }

            List<Cgd> matched = ForceMatchUtils.findFirstMatchByAmount(fp.getJshjBd(), candidates);
            if (!matched.isEmpty()
                    && DateMatchUtils.getDateDifference(matched, LocalDate.parse(fp.getKprq().substring(0, 10))) <= RfyyConstant.MAX_DATE_GAP) {
                MatchUtils.processMatchSuccess(
                        fp,
                        matched,
                        xsf,
                        ctx,
                        StrategyEnum.FORCE_MATCH_SPMC_JE,
                        MatchStatus.AUTO_MATCHED
                );
                fpIterator.remove();
            }

        }
        return xsf.allMatched();
    }

}
