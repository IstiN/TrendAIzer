package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.indicator.SuperTrendIndicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class AdvancedStrategy extends Strategy {

    public AdvancedStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        Double rsi = calcOrFromCache(new RSIIndicator(14), historicalData, Timeframe.M1);
        MACDIndicator.Result macdResult = calcOrFromCache(new MACDIndicator(12, 26, 9), historicalData, Timeframe.M1);
        Double superTrendSignal = calcOrFromCache(new SuperTrendIndicator(10, 3.0), historicalData, Timeframe.M1);
        if (rsi == null || macdResult == null || superTrendSignal == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
        if (superTrendSignal > 0 && rsi < 40 && macdResult.getMacd() > 0) {
            return new DecisionReason(Decision.LONG, "SuperTrend is bullish, RSI is below 40, and MACD is bullish");
        } else if (superTrendSignal < 0 && rsi > 60 && macdResult.getMacd() < 0) {
            return new DecisionReason(Decision.SHORT, "SuperTrend is bearish, RSI is above 60, and MACD is bearish");
        } else {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
    }
}