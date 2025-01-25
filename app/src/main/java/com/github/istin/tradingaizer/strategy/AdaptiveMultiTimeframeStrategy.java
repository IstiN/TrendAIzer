package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class AdaptiveMultiTimeframeStrategy extends Strategy {

    public AdaptiveMultiTimeframeStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Data aggregation for multiple timeframes
        List<? extends StatData> data5m = getData(historicalData, Timeframe.M5);
        List<? extends StatData> data15m = getData(historicalData, Timeframe.M15);
        List<? extends StatData> data1h = getData(historicalData, Timeframe.H1);

        if (data5m.isEmpty() || data15m.isEmpty() || data1h.isEmpty()) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }

        // Indicators for 5m timeframe
        MACDIndicator macd5m = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi5m = new RSIIndicator(14);
        SuperTrendIndicator superTrend5m = new SuperTrendIndicator(10, 3.0);
        ATRIndicator atr5m = new ATRIndicator(14);

        // Indicators for 15m timeframe
        MovingAverageIndicator shortMa15m = new MovingAverageIndicator(20);
        MovingAverageIndicator longMa15m = new MovingAverageIndicator(50);

        // Indicators for 1h timeframe
        RSIIndicator rsi1h = new RSIIndicator(14);

        // Calculate values for indicators
        MACDIndicator.Result macdValue5m = calcOrFromCache(macd5m, data5m, Timeframe.M5);
        Double rsiValue5m = calcOrFromCache(rsi5m, data5m, Timeframe.M5);
        Double superTrendSignal5m = calcOrFromCache(superTrend5m, data5m, Timeframe.M5);
        Double atrValue5m = calcOrFromCache(atr5m, data5m, Timeframe.M5);

        Double shortMaValue15m = calcOrFromCache(shortMa15m, data15m, Timeframe.M15);
        Double longMaValue15m = calcOrFromCache(longMa15m, data15m, Timeframe.M15);

        Double rsiValue1h = calcOrFromCache(rsi1h, data1h, Timeframe.H1);

        if (macdValue5m == null || rsiValue5m == null || superTrendSignal5m == null ||
                atrValue5m == null || shortMaValue15m == null || longMaValue15m == null || rsiValue1h == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }

        // Retrieve latest price
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Trend confirmation on 15m timeframe
        boolean isUptrend = shortMaValue15m > longMaValue15m && latestPrice > shortMaValue15m;
        boolean isDowntrend = shortMaValue15m < longMaValue15m && latestPrice < shortMaValue15m;

        // Stronger trend bias using 1h RSI
        boolean isStrongUptrend = isUptrend && rsiValue1h > 55;
        boolean isStrongDowntrend = isDowntrend && rsiValue1h < 45;

        // Volatility thresholds
        double atrThreshold = latestPrice * 0.004;
        boolean isLowVolatility = atrValue5m < atrThreshold;

        // ATR-based thresholds for exits
        double upperThreshold = latestPrice + (atrValue5m * 2);
        double lowerThreshold = latestPrice - (atrValue5m * 2);

        // Entry logic
        if (isStrongUptrend && macdValue5m.getMacd() > macdValue5m.getSignalLine() && rsiValue5m < 60 && superTrendSignal5m > 0 && isLowVolatility) {
            return new DecisionReason(Decision.LONG, "Strong uptrend, bullish MACD, and RSI < 60");
        }
        if (isStrongDowntrend && macdValue5m.getMacd() < macdValue5m.getSignalLine() && rsiValue5m > 40 && superTrendSignal5m < 0 && isLowVolatility) {
            return new DecisionReason(Decision.SHORT, "Strong downtrend, bearish MACD, and RSI > 40");
        }

        // Exit logic
        if (latestPrice > upperThreshold || latestPrice < lowerThreshold) {
            return new DecisionReason(Decision.CLOSE, "Price exceeded ATR thresholds");
        }

        // Default hold
        return new DecisionReason(Decision.HOLD, "No clear signal");
    }
}
