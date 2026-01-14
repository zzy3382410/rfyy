package com.current.rfyy.Strategy;

import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.MatchResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/9 9:29
 * @Description: TODO
 **/
@Data
public class MatchContext {

    // 命中结果集合
    private List<MatchResult> matchResults = new ArrayList<>();


    // 配置项，如阈值、最大耗时等
    private int stopThreshold;


    /**
     * 添加命中结果
     *
     * @param fp       发票
     * @param cgd      采购单
     * @param strategy 匹配策略
     * @param status   匹配状态
     */
    public void addMatched(Fp fp, Cgd cgd, String strategy, String status) {
        // 根据fpdm,fphm,sdphm,djbh判断结果集中是否已存在
        boolean exist = matchResults.stream().anyMatch(result -> result.getFpdm().equals(fp.getFpdm())
                && result.getFphm().equals(fp.getFphm())
                && result.getSdphm().equals(fp.getSdphm())
                && result.getDjbh().equals(cgd.getDjbh()));
        if (!exist) {
            matchResults.add(new MatchResult(
                    fp.getFpdm(),
                    fp.getFphm(),
                    fp.getSdphm(),
                    cgd.getDjbh(),
                    strategy,
                    status,
                    LocalDateTime.now(),
                    "system"
            ));
        }
    }
}
