package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.ForceMatchFpUtils;
import com.current.rfyy.utils.MatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 9:58
 * @Description: TODO 采购单匹配发票 一对多
 **/
@Component("forceMatchFpAlgorithm")
@RequiredArgsConstructor
public class ForceMatchFpAlgorithm implements MatchFpAlgorithm {


    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.FORCE_FP;
    }

    @Override
    public boolean matchFp(Cgd cgd,
                           XsfMatchData xsf,
                           MatchContext ctx,
                           List<Fp> filterFps,
                           StrategyEnum strategy) {

        List<Fp> result =
                ForceMatchFpUtils.findFirstMatchByAmount(cgd.getHjjeBd(), filterFps);

        if (!CollectionUtils.isEmpty(result)
                // && DateMatchUtils.getDateDifference(result, LocalDate.parse(cgd.getRq())) <= RfyyConstant.MAX_DATE_GAP
        ) {
            MatchUtils.processMatchSuccess(
                    cgd, result, xsf, ctx,
                    strategy + getType().getAlgorithm()+"_NEW_FORCE",
                    MatchStatus.AUTO_MATCHED
            );
            return true;
        }

        return false;
    }

}
