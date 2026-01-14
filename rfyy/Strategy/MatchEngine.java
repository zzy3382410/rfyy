package com.current.rfyy.Strategy;

import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.XsfMatchData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/1/14 14:51
 * @Description: TODO
 **/
@Slf4j
public class MatchEngine {
    /**
     * 策略注册表
     */
    private final Map<StrategyEnum, MatchStrategy> strategyRegistry =
            new EnumMap<>(StrategyEnum.class);

    /**
     * 策略执行顺序
     */
    private final List<StrategyEnum> executionOrder = new ArrayList<>();

    /**
     * 构造时完成注册
     */
    public MatchEngine() {
        registerDefaults();
    }

    /**
     * 默认注册（你也可以改成 Spring 注入）
     * 后续你补策略只需要在这里加
     */
    private void registerDefaults() {
        register(StrategyEnum.SPMC_PH_JE_SL, new MatchBySpmcAndPhAndJeAndSl());
        register(StrategyEnum.SPMC_JE_SL_RQ, new MatchBySpmcAndJeAndSlAndRq());
        register(StrategyEnum.SPMC_JE_SL, new MatchBySpmcAndJeAndSl());
        register(StrategyEnum.PH_JE_SL, new MatchByPhAndJeAndSl());
        register(StrategyEnum.PH_RQ_JE_SL, new MatchByPhAndRqAndJeAndSl());
        register(StrategyEnum.FPMX_CGDMX_SPMC_JE_SL, new MathchByFpMxAndCgdMx());
    }

    /**
     * 注册策略
     */
    public void register(StrategyEnum type, MatchStrategy strategy) {
        strategyRegistry.put(type, strategy);
        executionOrder.add(type);
    }

    /**
     * 执行匹配（单企业）
     */
    public MatchContext match(XsfMatchData xsf) {

        MatchContext ctx = new MatchContext();

        // 初始化 remaining 数据
        xsf.initRemaining();

        for (StrategyEnum type : executionOrder) {

            MatchStrategy strategy = strategyRegistry.get(type);
            if (strategy == null) {
                continue;
            }
            boolean done = false;
            try {
                done = strategy.match(xsf, ctx);
            } catch (Exception e) {
                log.error("企业：{},匹配策略:{} 执行异常: {}", xsf.getXfmc(), strategy.getClass().getSimpleName(), e.getMessage(), e);
            }
            // 已全部命中 or 策略要求短路
            if (done || xsf.allMatched()) {
                break;
            }

        }

        return ctx;
    }
}
