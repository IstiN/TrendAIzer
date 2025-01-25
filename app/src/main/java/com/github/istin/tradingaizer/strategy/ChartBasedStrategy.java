package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class ChartBasedStrategy extends Strategy {

    public ChartBasedStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Initialize indicators
        MACDIndicator macdIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsiIndicator = new RSIIndicator(14);
        SuperTrendIndicator superTrendIndicator = new SuperTrendIndicator(10, 3.0);
        ATRIndicator atrIndicator = new ATRIndicator(14); // ATR for stop-loss management
        MovingAverageIndicator maIndicator = new MovingAverageIndicator(50); // 50-period MA

        // Calculate indicator values
        MACDIndicator.Result macdResult = calcOrFromCache(macdIndicator, historicalData, Timeframe.M1);
        double rsi = calcOrFromCache(rsiIndicator, historicalData, Timeframe.M1);
        double superTrendSignal = calcOrFromCache(superTrendIndicator, historicalData, Timeframe.M1);
        double atr = calcOrFromCache(atrIndicator, historicalData, Timeframe.M1);
        double ma = calcOrFromCache(maIndicator, historicalData, Timeframe.M1);

        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Ensure the price is trending above/below the moving average for better entries
        boolean isUptrend = latestPrice > ma && ma > calcOrFromCache(maIndicator, historicalData.subList(0, historicalData.size() - 1), Timeframe.M1);
        boolean isDowntrend = latestPrice < ma && ma < calcOrFromCache(maIndicator, historicalData.subList(0, historicalData.size() - 1), Timeframe.M1);

        // Decision logic with ATR-based stop-loss
        if (superTrendSignal > 0 && macdResult.getMacd() > 0 && rsi < 55 && isUptrend) {
            // Long position: Strong trend, confirmed by SuperTrend, MACD, RSI, and moving average
            return new DecisionReason(Decision.LONG, "Strong uptrend, confirmed by SuperTrend, MACD, RSI, and moving average");
        } else if (superTrendSignal < 0 && macdResult.getMacd() < 0 && rsi > 45 && isDowntrend) {
            // Short position: Strong downtrend, confirmed by SuperTrend, MACD, RSI, and moving average
            return new DecisionReason(Decision.SHORT, "Strong downtrend, confirmed by SuperTrend, MACD, RSI, and moving average");
        } else {
            // Hold position: No strong signals
            return new DecisionReason(Decision.HOLD, "No strong signals");
        }
    }
}
