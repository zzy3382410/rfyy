package com.current.rfyy.MatchAlgorithm;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.MatchAlgorithmEnum;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.XsfMatchData;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/12 9:50
 * @Description: TODO 采购单匹配发票 一对多
 **/
public interface MatchFpAlgorithm {

    /**
     * 获取匹配算法类型
     *
     * @return 匹配算法类型
     */
    MatchAlgorithmEnum getType();

    /**
     * 匹配算法
     *
     * @param cgd            采购单
     * @param xsf           供应商
     * @param ctx           上下文
     * @param filterFps    过滤的发票
     * @param strategy      策略
     * @return 匹配结果
     */
    boolean matchFp(Cgd cgd,
                    XsfMatchData xsf,
                    MatchContext ctx,
                    List<Fp> filterFps,
                    StrategyEnum strategy);



}
