package com.current.rfyy.Strategy;

import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.XsfMatchData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/14 14:51
 * @Description: TODO
 **/
@Slf4j
@Component
public class MatchEngine {

    /**
     * 策略执行顺序
     */
    private final List<StrategyEnum> executionOrder = new ArrayList<>();

    /**
     * 策略工厂
     */
    private final StrategyFactory strategyFactory;

    /**
     * 构造函数
     */
    public MatchEngine(StrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
        registerDefaults();
    }


    /**
     * 默认注册（你也可以改成 Spring 注入）
     * 后续你补策略只需要在这里加
     */
    private void registerDefaults() {
        register(StrategyEnum.SPMC_PH);
        register(StrategyEnum.FORCE_MATCH_SPMC_PH);
        register(StrategyEnum.FORCE_MATCH_SPMC_PH_RQ);
        register(StrategyEnum.YZYF_SPMC_PH);
        // 1：1
        register(StrategyEnum.SPMC_MX);
        register(StrategyEnum.SPMC_PZWH);
        register(StrategyEnum.SPMC);
        // register(StrategyEnum.PH);
        register(StrategyEnum.FORCE_MATCH_SPMC_JE);
        //cgd -> fp
        register(StrategyEnum.CGD_SPMC_PH);
        register(StrategyEnum.CGD_SPMC);
        register(StrategyEnum.FORCE_MATCH_JE);


        // register(StrategyEnum.FPMX_CGDMX_SPMC_JE_SL, new MathchByFpMxAndCgdMx());
        // register(StrategyEnum.PH_SPMC_RQ_GROUP, new MatchByPhAndSpmcAndRqGroup());
    }

    /**
     * 注册策略
     */
    public void register(StrategyEnum type) {
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
            MatchStrategy strategy = strategyFactory.get(type);
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
