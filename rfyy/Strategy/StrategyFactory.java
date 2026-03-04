package com.current.rfyy.Strategy;

import com.current.rfyy.constant.StrategyEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zzy
 * @Date: 2026/2/12 14:26
 * @Description: TODO 策略工厂类
 **/
@Component
public class StrategyFactory {

    private final Map<StrategyEnum, MatchStrategy> strategyMap = new HashMap<>();

    public StrategyFactory(List<MatchStrategy> strategies) {
        for (MatchStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public MatchStrategy get(StrategyEnum type) {
        return strategyMap.get(type);
    }
}
