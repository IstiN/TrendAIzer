package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class OptimizedStrategy extends Strategy {

    private int rsiOverbought = 60;
    private int rsiOversold = 40;

    public OptimizedStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    public OptimizedStrategy(String cacheId, ChartDataProvider chartDataProvider, int rsiOverbought, int rsiOversold) {
        super(cacheId, chartDataProvider);
        this.rsiOverbought = rsiOverbought;
        this.rsiOversold = rsiOversold;
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Initialize indicators
        MACDIndicator macdIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsiIndicator = new RSIIndicator(14);
        ATRIndicator atrIndicator = new ATRIndicator(14);
        MovingAverageIndicator maIndicator = new MovingAverageIndicator(50);

        // Calculate indicator values
        MACDIndicator.Result macd = calcOrFromCache(macdIndicator, historicalData, Timeframe.M1);
        Double rsi = calcOrFromCache(rsiIndicator, historicalData, Timeframe.M1);
        Double atr = calcOrFromCache(atrIndicator, historicalData, Timeframe.M1);
        Double ma = calcOrFromCache(maIndicator, historicalData, Timeframe.M1);

        if (macd == null || rsi == null || atr == null || ma == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Trend confirmation using Moving Average
        boolean isUptrend = latestPrice > ma;
        boolean isDowntrend = latestPrice < ma;

        // ATR-based thresholds for better risk/reward
        double atrMultiplier = 1.5; // Adjusted multiplier for better sensitivity
        double upperThreshold = latestPrice + (atr * atrMultiplier); // Resistance
        double lowerThreshold = latestPrice - (atr * atrMultiplier); // Support

        if (isUptrend && macd.getMacd() > macd.getSignalLine() && rsi < rsiOverbought) {
            return new DecisionReason(Decision.LONG, "Uptrend, bullish MACD crossover, and RSI is not overbought");
        }

        // Short Entry Logic
        if (isDowntrend && macd.getMacd() < macd.getSignalLine() && rsi > rsiOversold) {
            return new DecisionReason(Decision.SHORT, "Downtrend, bearish MACD crossover, and RSI is not oversold");
        }

        // Exit (Close) Logic
        if ((macd.getMacd() < macd.getSignalLine() && isUptrend) || (macd.getMacd() > macd.getSignalLine() && isDowntrend) || (latestPrice > upperThreshold || latestPrice < lowerThreshold)) {
            return new DecisionReason(Decision.CLOSE, "MACD crossover against trend or price beyond ATR threshold");
        }

        // Default Hold
        return new DecisionReason(Decision.HOLD, "No clear signal");
    }
}