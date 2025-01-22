package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.indicator.CachedIndicator;
import com.github.istin.tradingaizer.indicator.Indicator;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public abstract class Strategy {

    private final String cacheId;
    private final CachedIndicator indicator;

    public Strategy(String cacheId) {
        this.cacheId = cacheId;
        this.indicator = new CachedIndicator(cacheId);
    }

    double calcOrFromCache(Indicator param, List<KlineData> historicalData) {
        indicator.setIndicator(param);
        return indicator.calculate(historicalData);
    }

    public abstract Decision generateDecision(List<KlineData> historicalData);
}
