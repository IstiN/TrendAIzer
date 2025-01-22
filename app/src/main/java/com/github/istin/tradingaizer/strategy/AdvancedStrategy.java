package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class AdvancedStrategy extends Strategy {

    public AdvancedStrategy(String cacheId) {
        super(cacheId);
    }

    @Override
    public Decision generateDecision(List<KlineData> historicalData) {
        double rsi = calcOrFromCache(new RSIIndicator(14), historicalData);
        double macd = calcOrFromCache(new MACDIndicator(12, 26, 9), historicalData);
        double superTrendSignal = calcOrFromCache(new SuperTrendIndicator(10, 3.0), historicalData);

        if (superTrendSignal > 0 && rsi < 40 && macd > 0) {
            return Decision.LONG;
        } else if (superTrendSignal < 0 && rsi > 60 && macd < 0) {
            return Decision.SHORT;
        } else {
            return Decision.HOLD;
        }
    }
}