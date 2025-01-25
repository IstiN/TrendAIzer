package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class EnhancedStrategy2 extends Strategy {

    public EnhancedStrategy2(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Aggregate data into different timeframes
        List<StatData> data5m = TimeframeAggregator.convertToTimeframe(historicalData, Timeframe.M5);  // 5-minute data
        List<StatData> data15m = TimeframeAggregator.convertToTimeframe(historicalData, Timeframe.M15); // 15-minute data

        // Initialize indicators for 5m data
        MACDIndicator macd5m = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi5m = new RSIIndicator(14);
        SuperTrendIndicator superTrend5m = new SuperTrendIndicator(10, 3.0);
        ATRIndicator atr5m = new ATRIndicator(14);

        // Initialize indicators for 15m data
        MovingAverageIndicator shortMa15m = new MovingAverageIndicator(20);
        MovingAverageIndicator longMa15m = new MovingAverageIndicator(50);

        // Calculate indicators for 5m data
        MACDIndicator.Result macdValue = calcOrFromCache(macd5m, data5m, Timeframe.M5);
        double rsiValue = calcOrFromCache(rsi5m, data5m, Timeframe.M5);
        double superTrendSignal = calcOrFromCache(superTrend5m, data5m, Timeframe.M5);
        double atrValue = calcOrFromCache(atr5m, data5m, Timeframe.M5);

        // Calculate indicators for 15m data
        double shortMaValue = calcOrFromCache(shortMa15m, data15m, Timeframe.M15);
        double longMaValue = calcOrFromCache(longMa15m, data15m, Timeframe.M15);

        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Determine 15m trend
        boolean isUptrend15m = shortMaValue > longMaValue && latestPrice > shortMaValue;
        boolean isDowntrend15m = shortMaValue < longMaValue && latestPrice < shortMaValue;

        // Calculate ATR-based thresholds
        double upperThreshold = latestPrice + (atrValue * 2);
        double lowerThreshold = latestPrice - (atrValue * 2);

        // Entry logic
        if (isUptrend15m && macdValue.getMacd() > macdValue.getSignalLine() && rsiValue < 60 && superTrendSignal > 0) {
            return new DecisionReason(Decision.LONG, "Uptrend, MACD bullish, RSI below 60, and SuperTrend is bullish");
        } else if (isDowntrend15m && macdValue.getMacd() < macdValue.getSignalLine() && rsiValue > 40 && superTrendSignal < 0) {
            return new DecisionReason(Decision.SHORT, "Downtrend, MACD bearish, RSI above 40, and SuperTrend is bearish");
        }

        // Exit logic
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
