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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/12 11:43
 * @Description: TODO rq匹配算法
 **/

@Component("rqGroupMatchFpAlgorithm")
@RequiredArgsConstructor
public class RqGroupMatchFpAlgorithm implements MatchFpAlgorithm {


    @Override
    public MatchAlgorithmEnum getType() {
        return MatchAlgorithmEnum.RQ_GROUP_FP;
    }

    @Override
    public boolean matchFp(Cgd cgd,
                           XsfMatchData xsf,
                           MatchContext ctx,
                           List<Fp> filterFps,
                           StrategyEnum strategy) {
        Map<String, List<Fp>> rqGroup = filterFps.stream()
                .collect(Collectors.groupingBy(fp -> fp.getKprq().substring(0, 10)));

        List<String> matchedRq = new ArrayList<>();
        for (Map.Entry<String, List<Fp>> entry : rqGroup.entrySet()) {
            if (CalcUtils.compareJe(cgd.getHjjeBd(), CalcUtils.calcFpJe(entry.getValue()))
                    && DateMatchUtils.getFpDateDifference(entry.getValue(), LocalDate.parse(cgd.getRq())) <= RfyyConstant.MAX_DATE_GAP) {
                matchedRq.add(entry.getKey());
            }
        }
        // 取日期相隔最近的结果
        if (!matchedRq.isEmpty()) {
            String closestRq = DateMatchUtils.findClosestRq(matchedRq, cgd.getRq());
            List<Fp> matchedCgds = rqGroup.get(closestRq);
            MatchUtils.processMatchSuccess(
                    cgd,
                    matchedCgds,
                    xsf,
                    ctx,
                    strategy + getType().getAlgorithm(),
                    MatchStatus.AUTO_MATCHED);
            return true;
        }

        return false;
    }

}
