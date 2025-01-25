package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class EnhancedStrategy extends Strategy {

    public EnhancedStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Aggregate data into different timeframes
        List<? extends StatData> data5m = getData(historicalData, Timeframe.M5);  // 5-minute data
        List<? extends StatData> data15m = getData(historicalData, Timeframe.M15); // 15-minute data
        List<? extends StatData> data1h = getData(historicalData, Timeframe.H1); // 1-hour data

        if (data5m.isEmpty() || data15m.isEmpty() || data1h.isEmpty()) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
        // Initialize indicators for 5m data
        MACDIndicator macd5m = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi5m = new RSIIndicator(14);
        SuperTrendIndicator superTrend5m = new SuperTrendIndicator(10, 3.0);
        ATRIndicator atr5m = new ATRIndicator(14);

        // Initialize indicators for 15m data
        MovingAverageIndicator shortMa15m = new MovingAverageIndicator(20); // Short-term MA
        MovingAverageIndicator longMa15m = new MovingAverageIndicator(50); // Long-term MA

        // Initialize indicators for 1h data
        RSIIndicator rsi1h = new RSIIndicator(14); // RSI for higher timeframe filtering

        // Calculate indicators for 5m data
        MACDIndicator.Result macdValue = calcOrFromCache(macd5m, data5m, Timeframe.M5);
        Double rsi5mValue = calcOrFromCache(rsi5m, data5m, Timeframe.M5);
        Double superTrendSignal = calcOrFromCache(superTrend5m, data5m, Timeframe.M5);
        Double atrValue = calcOrFromCache(atr5m, data5m, Timeframe.M5);

        // Calculate indicators for 15m data
        Double shortMaValue15m = calcOrFromCache(shortMa15m, data15m, Timeframe.M15);
        Double longMaValue15m = calcOrFromCache(longMa15m, data15m, Timeframe.M15);

        // Calculate indicators for 1h data
        Double rsi1hValue = calcOrFromCache(rsi1h, data1h, Timeframe.H1);

        if (macdValue == null || rsi5mValue == null || superTrendSignal == null || atrValue == null || shortMaValue15m == null || longMaValue15m == null || rsi1hValue == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }

        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Determine 15m trend
        boolean isUptrend15m = shortMaValue15m > longMaValue15m && latestPrice > shortMaValue15m;
        boolean isDowntrend15m = shortMaValue15m < longMaValue15m && latestPrice < shortMaValue15m;

        // Strong trend confirmation with higher timeframe (1h RSI)
        boolean isStrongUptrend = isUptrend15m && rsi1hValue > 55;
        boolean isStrongDowntrend = isDowntrend15m && rsi1hValue < 45;

        // Volatility filter based on ATR
        double atrThreshold = latestPrice * 0.004; // ATR must be within 0.4% of price
        boolean isLowVolatility = atrValue < atrThreshold;

        // Calculate ATR-based thresholds for exit conditions
        double upperThreshold = latestPrice + (atrValue * 2);
        double lowerThreshold = latestPrice - (atrValue * 2);

        // Entry logic with stricter conditions
        if (isStrongUptrend && macdValue.getMacd() > macdValue.getSignalLine() && rsi5mValue < 60 && superTrendSignal > 0 && isLowVolatility) {
            return new DecisionReason(Decision.LONG, "Strong uptrend, bullish MACD, RSI < 60, and SuperTrend is bullish");
        } else if (isStrongDowntrend && macdValue.getMacd() < macdValue.getSignalLine() && rsi5mValue > 40 && superTrendSignal < 0 && isLowVolatility) {
            return new DecisionReason(Decision.SHORT, "Strong downtrend, bearish MACD, RSI > 40, and SuperTrend is bearish");
        }

        // Exit logic with tighter thresholds and trend checks
        if ((macdValue.getMacd() < macdValue.getSignalLine() && isStrongUptrend) ||
                (macdValue.getMacd() > macdValue.getSignalLine() && isStrongDowntrend) ||
                (latestPrice > upperThreshold || latestPrice < lowerThreshold) ||
                (rsi5mValue > 70 && isStrongUptrend) ||
                (rsi5mValue < 30 && isStrongDowntrend)) {
            return new DecisionReason(Decision.CLOSE, "Exit due to opposite MACD signal, price breaks ATR thresholds, or RSI overbought/oversold");
        }

        // Default hold decision
        return new DecisionReason(Decision.HOLD, "No clear signal");
    }
}
