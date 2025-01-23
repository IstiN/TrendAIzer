package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.indicator.SuperTrendIndicator;
import com.github.istin.tradingaizer.indicator.ATRIndicator; // New ATR indicator
import com.github.istin.tradingaizer.indicator.MovingAverageIndicator; // Moving average indicator
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class ChartBasedStrategy extends Strategy {

    public ChartBasedStrategy(String cacheId) {
        super(cacheId);
    }

    @Override
    public Decision generateDecision(List<KlineData> historicalData) {
        // Initialize indicators
        MACDIndicator macdIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsiIndicator = new RSIIndicator(14);
        SuperTrendIndicator superTrendIndicator = new SuperTrendIndicator(10, 3.0);
        ATRIndicator atrIndicator = new ATRIndicator(14); // ATR for stop-loss management
        MovingAverageIndicator maIndicator = new MovingAverageIndicator(50); // 50-period MA

        // Calculate indicator values
        double macd = calcOrFromCache(macdIndicator, historicalData);
        double rsi = calcOrFromCache(rsiIndicator, historicalData);
        double superTrendSignal = calcOrFromCache(superTrendIndicator, historicalData);
        double atr = calcOrFromCache(atrIndicator, historicalData);
        double ma = calcOrFromCache(maIndicator, historicalData);

        // Retrieve the latest data
        KlineData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Ensure the price is trending above/below the moving average for better entries
        boolean isUptrend = latestPrice > ma && ma > calcOrFromCache(maIndicator, historicalData.subList(0, historicalData.size() - 1));
        boolean isDowntrend = latestPrice < ma && ma < calcOrFromCache(maIndicator, historicalData.subList(0, historicalData.size() - 1));

        // Decision logic with ATR-based stop-loss
        if (superTrendSignal > 0 && macd > 0 && rsi < 55 && isUptrend) {
            // Long position: Strong trend, confirmed by SuperTrend, MACD, RSI, and moving average
            return Decision.LONG;
        } else if (superTrendSignal < 0 && macd < 0 && rsi > 45 && isDowntrend) {
            // Short position: Strong downtrend, confirmed by SuperTrend, MACD, RSI, and moving average
            return Decision.SHORT;
        } else {
            // Hold position: No strong signals
            return Decision.HOLD;
        }
    }
}
