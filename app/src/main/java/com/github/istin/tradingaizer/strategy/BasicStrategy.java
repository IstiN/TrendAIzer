package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class BasicStrategy extends Strategy {

    public BasicStrategy(String cacheId) {
        super(cacheId);
    }

    @Override
    public Decision generateDecision(List<KlineData> historicalData) {
        double rsi = calcOrFromCache(new RSIIndicator(14), historicalData);
        double macd = calcOrFromCache(new MACDIndicator(12, 26, 9), historicalData);

        if (rsi < 40 && macd > 0) {
            return Decision.LONG;
        } else if (rsi > 60 && macd < 0) {
            return Decision.SHORT;
        } else {
            return Decision.HOLD;
        }
    }

}
