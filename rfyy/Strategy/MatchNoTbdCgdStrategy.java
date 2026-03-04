// package com.current.rfyy.Strategy;
//
// import com.current.rfyy.MatchAlgorithm.MatchAlgorithm;
// import com.current.rfyy.constant.StrategyEnum;
// import com.current.rfyy.domain.Cgd;
// import com.current.rfyy.domain.Fp;
// import com.current.rfyy.domain.XsfMatchData;
// import com.current.rfyy.utils.DataFilterUtils;
// import com.current.rfyy.utils.MatchUtils;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
// import org.springframework.util.CollectionUtils;
//
// import java.util.Iterator;
// import java.util.List;
// import java.util.Map;
//
// /**
//  * @Author: zzy
//  * @Date: 2026/1/9 10:22
//  * @Description: TODO SPMC_PH
//  **/
// @Slf4j
// @Component
// public class MatchNoTbdCgdStrategy implements MatchStrategy {
//
//     @Override
//     public StrategyEnum getType() {
//         return StrategyEnum.SPMC_PH;
//     }
//
//     private final Map<String, MatchAlgorithm> algorithmMap;
//
//     public MatchNoTbdCgdStrategy(Map<String, MatchAlgorithm> algorithmMap) {
//         this.algorithmMap = algorithmMap;
//     }
//
//
//     @Override
//     public boolean match(XsfMatchData xsf, MatchContext ctx) {
//
//         if (xsf.allMatched()) {
//             return true;
//         }
//
//         List<Fp> remainingFps = xsf.getRemainingFp();
//         List<Cgd> remainingCgds = xsf.getRemainingCgd();
//
//         Iterator<Fp> iterator = remainingFps.iterator();
//
//         while (iterator.hasNext()) {
//
//             Fp fp = iterator.next();
//
//             if (fp.isMatched()
//                     || !MatchUtils.isValid(fp.getHandledSpmc(), fp.getBz())) {
//                 continue;
//             }
//
//             // 只过滤一次
//             List<Cgd> matchedCgds =
//                     DataFilterUtils.filterCgdsBySpmcAndPh(fp, remainingCgds, fp.getPhs());
//
//             if (CollectionUtils.isEmpty(matchedCgds)) {
//                 continue;
//             }
//             boolean matched = false;
//             //todo 匹配没有退补单的入库单
//
//
//             if (matched) {
//                 iterator.remove();
//             }
//         }
//
//         return xsf.allMatched();
//     }
//
//
// }
//
