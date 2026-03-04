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
 * @Description: TODO
 **/
public interface MatchAlgorithm {

    /**
     * 获取匹配算法类型
     *
     * @return 匹配算法类型
     */
    MatchAlgorithmEnum getType();

    /**
     * 匹配算法
     *
     * @param fp            发票
     * @param xsf           供应商
     * @param ctx           上下文
     * @param filterCgds    过滤的采购单
     * @param remainingCgds 剩余的采购单
     * @return 匹配结果
     */
    boolean match(Fp fp,
                  XsfMatchData xsf,
                  MatchContext ctx,
                  List<Cgd> filterCgds,
                  List<Cgd> remainingCgds,
                  StrategyEnum strategy);


    /**
     * 匹配算法(带尾差)
     *
     * @param fp            发票
     * @param xsf           供应商
     * @param ctx           上下文
     * @param filterCgds    过滤的采购单
     * @param remainingCgds 剩余的采购单
     * @return 匹配结果
     */
    boolean matchWithWc(Fp fp,
                        XsfMatchData xsf,
                        MatchContext ctx,
                        List<Cgd> filterCgds,
                        List<Cgd> remainingCgds,
                        StrategyEnum strategy);
}
