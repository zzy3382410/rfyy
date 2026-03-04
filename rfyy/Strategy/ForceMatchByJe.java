package com.current.rfyy.Strategy;

import com.current.renfu.tool.RuleTools;
import com.current.rfyy.constant.MatchStatus;
import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.CgdMx;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;
import com.current.rfyy.utils.CalcUtils;
import com.current.rfyy.utils.MatchUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/10 10:43
 * @Description: TODO  FORCE_MATCH_JE
 **/
@Component
public class ForceMatchByJe implements MatchStrategy {

    @Override
    public StrategyEnum getType() {
        return StrategyEnum.FORCE_MATCH_JE;
    }


    @Override
    public boolean match(XsfMatchData xsf, MatchContext ctx) {

        if (xsf.allMatched()) {
            return true;
        }

        Iterator<Fp> fpIterator = xsf.getRemainingFp().iterator();
        List<Cgd> remainingCgds = xsf.getRemainingCgd();

        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();

            if (fp.isMatched() || StringUtils.isBlank(fp.getHandledSpmc())) {
                continue;
            }

            List<Cgd> candidates = preprocess(remainingCgds, fp);
            if (candidates == null || candidates.isEmpty()
                    || candidates.size() > RfyyConstant.MAX_CANDIDATE_SIZE) {
                continue;
            }

            List<Cgd> matched = findFirstMatch(fp, candidates);
            if (matched != null) {
                MatchUtils.processMatchSuccess(
                        fp,
                        matched,
                        xsf,
                        ctx,
                        StrategyEnum.FORCE_MATCH_JE,
                        MatchStatus.AUTO_MATCHED
                );
                fpIterator.remove();
            }
        }
        return xsf.allMatched();
    }

    /**
     * 预处理：时间 + 金额粗筛
     */
    private List<Cgd> preprocess(List<Cgd> cgds, Fp fp) {

        List<Cgd> filtered = cgds;

        if (filtered.size() > 30) {
            filtered = filterByDate(filtered, fp.getKprq(), fp.getHandledSpmc(), 30);
        }
        if (filtered == null || filtered.isEmpty()) {
            return null;
        }

        // 金额降序（利于早命中）
        filtered.sort((a, b) ->
                b.getHjjeBd().compareTo(a.getHjjeBd()));

        BigDecimal target = fp.getJshjBd();

        // 金额下限剪枝
        BigDecimal min = filtered.stream()
                .map(Cgd::getHjjeBd)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<Cgd> result = new ArrayList<>();
        for (Cgd cgd : filtered) {
            if (cgd.getHjjeBd().add(min).compareTo(target) <= 0) {
                result.add(cgd);
            }
        }
        return result;
    }

    /**
     * 查找第一个金额匹配组合
     */
    private List<Cgd> findFirstMatch(Fp fp, List<Cgd> cgds) {

        BigDecimal target = fp.getJshjBd();
        int n = cgds.size();

        BigDecimal[] suffix = new BigDecimal[n];
        suffix[n - 1] = cgds.get(n - 1).getHjjeBd();
        for (int i = n - 2; i >= 0; i--) {
            suffix[i] = suffix[i + 1].add(cgds.get(i).getHjjeBd());
        }

        List<Cgd> path = new ArrayList<>();
        if (dfs(cgds, 0, BigDecimal.ZERO, target, suffix, path)) {
            return new ArrayList<>(path);
        }
        return null;
    }

    /**
     * DFS + 剪枝
     */
    private boolean dfs(List<Cgd> cgds,
                        int index,
                        BigDecimal current,
                        BigDecimal target,
                        BigDecimal[] suffix,
                        List<Cgd> path) {

        if (CalcUtils.compareJe(current, target)) {
            return true;
        }

        if (current.compareTo(target) > 0) {
            return false;
        }

        for (int i = index; i < cgds.size(); i++) {

            // 剩余最大金额不足，直接剪
            if (current.add(suffix[i]).compareTo(target) < 0) {
                return false;
            }

            Cgd cgd = cgds.get(i);
            BigDecimal next = current.add(cgd.getHjjeBd());

            path.add(cgd);
            if (dfs(cgds, i + 1, next, target, suffix, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    /**
     * 时间范围递归过滤（带兜底）
     */
    private List<Cgd> filterByDate(List<Cgd> cgds,
                                   String kprq,
                                   String spmc,
                                   int days) {

        if (cgds.size() <= 30) {
            return cgds;
        }
        if (days <= 0) {
            return filterBySpmc(cgds, spmc);
        }

        List<Cgd> filtered = new ArrayList<>();
        for (Cgd cgd : cgds) {
            for (CgdMx mx : cgd.getCgdMxList()) {
                if (RuleTools.getDateDifference(mx.getRq(), kprq.substring(0, 10)) < days) {
                    filtered.add(cgd);
                    break;
                }
            }
        }
        return filterByDate(filtered, kprq, spmc, days / 2);
    }

    /**
     * 商品名兜底过滤
     */
    private List<Cgd> filterBySpmc(List<Cgd> cgds, String spmc) {
        List<Cgd> result = new ArrayList<>();
        for (Cgd cgd : cgds) {
            for (CgdMx mx : cgd.getCgdMxList()) {
                if (spmc.contains(mx.getHandledSpmc())) {
                    result.add(cgd);
                    break;
                }
            }
        }
        return result.size() > 30 ? null : result;
    }
}
