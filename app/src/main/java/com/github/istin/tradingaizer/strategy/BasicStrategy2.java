package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.ADXIndicator;
import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.indicator.SuperTrendIndicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class BasicStrategy2 extends Strategy {

    public BasicStrategy2(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {

        Double rsi = calcOrFromCache(new RSIIndicator(14), historicalData, Timeframe.M1);
        MACDIndicator.Result macdResult = calcOrFromCache(new MACDIndicator(12, 26, 9), historicalData, Timeframe.M1);
        Double adx = calcOrFromCache(new ADXIndicator(14), historicalData, Timeframe.M1);
        Double superTrendValue = calcOrFromCache(new SuperTrendIndicator(10, 2.0), historicalData, Timeframe.M1);

        // Ensure all indicator values are available:
        if (rsi == null || macdResult == null || adx == null || superTrendValue == null) {
            return new DecisionReason(Decision.HOLD, "Not enough data or no clear signal");
        }

        // Interpreting the return from SuperTrendIndicator:
        // > 0 => uptrend, < 0 => downtrend
        boolean isUptrend = superTrendValue > 0;
        boolean isDowntrend = superTrendValue < 0;

        // Example conditions:
        if (rsi < 35 && macdResult.getMacd() > 0 && adx > 20 && isUptrend) {
            return new DecisionReason(Decision.LONG, "RSI<35 & MACD>0 & ADX>20 & SuperTrend up");
        } else if (rsi > 65 && macdResult.getMacd() < 0 && adx > 20 && isDowntrend) {
            return new DecisionReason(Decision.SHORT, "RSI>65 & MACD<0 & ADX>20 & SuperTrend down");
        } else {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
    }
}
