package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class EnhancedStrategy1 extends Strategy {

    public EnhancedStrategy1(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Aggregate data into different timeframes
        List<? extends StatData> data5m = getData(historicalData, Timeframe.M5);  // 5-minute data
        List<? extends StatData> data15m = getData(historicalData, Timeframe.M15);; // 15-minute data
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

        // Calculate indicators on 5m data
        MACDIndicator.Result macdValue = calcOrFromCache(macd5m, data5m, Timeframe.M5);
        Double rsiValue = calcOrFromCache(rsi5m, data5m, Timeframe.M5);
        Double superTrendSignal = calcOrFromCache(superTrend5m, data5m, Timeframe.M5);
        Double atrValue = calcOrFromCache(atr5m, data5m, Timeframe.M5);

        // Calculate indicators on 15m data
        Double shortMaValue = calcOrFromCache(shortMa15m, data15m, Timeframe.M15);
        Double longMaValue = calcOrFromCache(longMa15m, data15m, Timeframe.M15);

        if (macdValue == null || rsiValue == null || superTrendSignal == null || atrValue == null || shortMaValue == null || longMaValue == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Determine trends from 15m data
        boolean isUptrend15m = shortMaValue > longMaValue && latestPrice > shortMaValue;
        boolean isDowntrend15m = shortMaValue < longMaValue && latestPrice < shortMaValue;

        // Volatility filter based on ATR
        double atrThreshold = latestPrice * 0.005; // Example: ATR must be within 0.5% of price
        boolean isLowVolatility = atrValue < atrThreshold;

        // Calculate ATR-based thresholds
        double upperThreshold = latestPrice + (atrValue * 1.5);
        double lowerThreshold = latestPrice - (atrValue * 1.5);

        // Entry logic with additional filters
        if (isUptrend15m && macdValue.getMacd() > macdValue.getSignalLine() && rsiValue < 60 && superTrendSignal > 0 && isLowVolatility) {
            return new DecisionReason(Decision.LONG, "Uptrend, bullish MACD, RSI < 60, SuperTrend > 0, Low Volatility");
        } else if (isDowntrend15m && macdValue.getMacd() < macdValue.getSignalLine() && rsiValue > 40 && superTrendSignal < 0 && isLowVolatility) {
            return new DecisionReason(Decision.SHORT, "Downtrend, bearish MACD, RSI > 40, SuperTrend < 0, Low Volatility");
        }

        // Exit logic with tighter conditions
        if ((macdValue.getMacd() < macdValue.getSignalLine() && isUptrend15m) ||
                (macdValue.getMacd() > macdValue.getSignalLine() && isDowntrend15m) ||
                (latestPrice > upperThreshold || latestPrice < lowerThreshold) ||
                (rsiValue > 70 && isUptrend15m) ||
                (rsiValue < 30 && isDowntrend15m)) {
            return new DecisionReason(Decision.CLOSE, "Exit signal");
        }

        // Default hold decision
        return new DecisionReason(Decision.HOLD, "No clear signal");
    }

}
