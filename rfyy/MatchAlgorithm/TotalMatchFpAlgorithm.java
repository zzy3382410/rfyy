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

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 9:58
 * @Description: TODO 采购单匹配发票 一对多
 **/
@Component("totalMatchFpAlgorithm")
@RequiredArgsConstructor
public class TotalMatchFpAlgorithm implements MatchFpAlgorithm {


    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.TOTAL_FP;
    }

    @Override
    public boolean matchFp(Cgd cgd,
                              XsfMatchData xsf,
                              MatchContext ctx,
                              List<Fp> filterFps,
                              StrategyEnum strategy) {
        if (CalcUtils.compareJe(cgd.getHjjeBd(), CalcUtils.calcFpJe(filterFps))
                && filterFps.stream().allMatch(fp ->
                DateMatchUtils.getDateDifference(cgd.getRq(), fp.getKprq()) <= RfyyConstant.MAX_DATE_GAP)) {
            MatchUtils.processMatchSuccess(
                    cgd, filterFps, xsf, ctx,
                    strategy + "_TOTAL",
                    MatchStatus.AUTO_MATCHED);
            return true;
        }
        return false;
    }

}
