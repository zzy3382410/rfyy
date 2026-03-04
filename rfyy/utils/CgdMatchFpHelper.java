package com.current.rfyy.utils;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/26 11:22
 * @Description: TODO
 **/
public class CgdMatchFpHelper {

    /**
     * 合计匹配
     */
    public boolean totalMatch(Cgd cgd,
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
