package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.Indicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public abstract class Strategy {

    private final String cacheId;
    private final ChartDataProvider chartDataProvider;

    public Strategy(String cacheId, ChartDataProvider chartDataProvider) {
        this.cacheId = cacheId;
        this.chartDataProvider = chartDataProvider;
    }

    <Result> Result calcOrFromCache(Indicator<Result> indicator, List<? extends StatData> historicalData, Timeframe timeframe) {
        return chartDataProvider.calculateIndicator(indicator, historicalData, timeframe);
    }

    protected List<? extends StatData> getData(List<? extends StatData> historicalData, Timeframe timeframe) {
        return chartDataProvider.getData(historicalData, timeframe);
    }

    public abstract DecisionReason generateDecision(List<? extends StatData> historicalData);
}
