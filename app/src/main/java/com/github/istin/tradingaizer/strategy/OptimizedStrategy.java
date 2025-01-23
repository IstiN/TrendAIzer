package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.indicator.ATRIndicator;
import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.MovingAverageIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class OptimizedStrategy extends Strategy {

    public OptimizedStrategy(String cacheId) {
        super(cacheId);
    }

    @Override
    public Decision generateDecision(List<KlineData> historicalData) {
        // Initialize indicators
        MACDIndicator macdIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsiIndicator = new RSIIndicator(14);
        ATRIndicator atrIndicator = new ATRIndicator(14);
        MovingAverageIndicator maIndicator = new MovingAverageIndicator(50);

        // Calculate indicator values
        double macd = calcOrFromCache(macdIndicator, historicalData);
        double macdSignal = macdIndicator.calculateSignalLine(historicalData);
        double rsi = calcOrFromCache(rsiIndicator, historicalData);
        double atr = calcOrFromCache(atrIndicator, historicalData);
        double ma = calcOrFromCache(maIndicator, historicalData);

        // Retrieve the latest data
        KlineData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Trend confirmation using Moving Average
        boolean isUptrend = latestPrice > ma;
        boolean isDowntrend = latestPrice < ma;

        // ATR-based thresholds for better risk/reward
        double atrMultiplier = 1.5; // Adjusted multiplier for better sensitivity
        double upperThreshold = latestPrice + (atr * atrMultiplier); // Resistance
        double lowerThreshold = latestPrice - (atr * atrMultiplier); // Support

        // Long Entry Logic
        if (isUptrend && macd > macdSignal && rsi < 60) {
            return Decision.LONG; // Enter long if uptrend, bullish MACD crossover, and RSI is not overbought
        }

        // Short Entry Logic
        if (isDowntrend && macd < macdSignal && rsi > 40) {
            return Decision.SHORT; // Enter short if downtrend, bearish MACD crossover, and RSI is not oversold
        }

        // Exit (Close) Logic
        if ((macd < macdSignal && isUptrend) || (macd > macdSignal && isDowntrend) || (latestPrice > upperThreshold || latestPrice < lowerThreshold)) {
            return Decision.CLOSE; // Exit if opposite MACD signal, or price breaks ATR thresholds
        }

        // Default Hold
        return Decision.HOLD;
    }
}