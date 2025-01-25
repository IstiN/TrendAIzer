package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class EnhancedStrategyV2 extends Strategy {

    public EnhancedStrategyV2(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Aggregate data into different timeframes
        List<? extends StatData> data5m = getData(historicalData, Timeframe.M5);
        List<? extends StatData> data15m = getData(historicalData, Timeframe.M15);
        List<? extends StatData> data1h = getData(historicalData, Timeframe.H1);

        if (data5m.isEmpty() || data15m.isEmpty() || data1h.isEmpty()) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }

        // Initialize indicators for 5m data
        MACDIndicator macd5mIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi5mIndicator = new RSIIndicator(14);
        BollingerBandsIndicator bollingerBands5mIndicator = new BollingerBandsIndicator(20, 2.0);
        SuperTrendIndicator superTrend5mIndicator = new SuperTrendIndicator(10, 3.0);

        // Initialize indicators for 15m data
        MovingAverageIndicator ema15mIndicator = new MovingAverageIndicator(50); // EMA for trend determination

        // Initialize indicators for 1h data
        MACDIndicator macd1hIndicator = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi1hIndicator = new RSIIndicator(14);

        // Calculate indicators on 5m data
        MACDIndicator.Result macdValue5m = calcOrFromCache(macd5mIndicator, data5m, Timeframe.M5);
        Double rsiValue5m = calcOrFromCache(rsi5mIndicator, data5m, Timeframe.M5);

        BollingerBandsIndicator.Result bollingerBands5mResult = calcOrFromCache(bollingerBands5mIndicator, data5m, Timeframe.M5);

        Double superTrendSignal5m = calcOrFromCache(superTrend5mIndicator, data5m, Timeframe.M5);
        if (macdValue5m == null || rsiValue5m == null || bollingerBands5mResult == null || superTrendSignal5m == null) {
            return new DecisionReason(Decision.HOLD, "No clear entry or exit conditions met.");
        }

        double bollingerUpper = bollingerBands5mResult.getUpperBand();
        double bollingerLower = bollingerBands5mResult.getLowerBand();

        // Log 5m indicators
        System.out.println("5m Indicators:");
        System.out.println("MACD: " + macdValue5m.getMacd() + ", Signal Line: " + macdValue5m.getSignalLine());
        System.out.println("RSI: " + rsiValue5m);
        System.out.println("Bollinger Bands - Upper: " + bollingerUpper + ", Lower: " + bollingerLower);
        System.out.println("SuperTrend Signal: " + superTrendSignal5m);

        // Calculate indicators on 15m data
        Double emaValue15m = calcOrFromCache(ema15mIndicator, data15m, Timeframe.M15);

        // Log 15m indicators
        System.out.println("15m Indicators:");
        System.out.println("EMA: " + emaValue15m);

        // Calculate indicators on 1h data
        MACDIndicator.Result macdValue1h = calcOrFromCache(macd1hIndicator, data1h, Timeframe.H1);
        Double rsiValue1h = calcOrFromCache(rsi1hIndicator, data1h, Timeframe.H1);

        if (emaValue15m == null || macdValue1h == null || rsiValue1h == null) {
            return new DecisionReason(Decision.HOLD, "No clear entry or exit conditions met.");
        }
        // Log 1h indicators
        System.out.println("1h Indicators:");
        System.out.println("MACD: " + macdValue1h.getMacd() + ", Signal Line: " + macdValue1h.getSignalLine());
        System.out.println("RSI: " + rsiValue1h);

        // Retrieve the latest data
        StatData latestData = historicalData.get(historicalData.size() - 1);
        double latestPrice = latestData.getClosePrice();

        // Enhanced logic for decision-making

        double macd5m = macdValue5m.getMacd();
        double signalLine5m = macdValue5m.getSignalLine();
        boolean isBullish5m = macd5m > signalLine5m && rsiValue5m < 60 && latestPrice > emaValue15m;
        boolean isBearish5m = macd5m < signalLine5m && rsiValue5m > 40 && latestPrice < emaValue15m;

        double macd1h = macdValue1h.getMacd();
        double signalLine1h = macdValue1h.getSignalLine();
        boolean isBullish1h = macd1h > signalLine1h && rsiValue1h < 60;
        boolean isBearish1h = macd1h < signalLine1h && rsiValue1h > 40;

        boolean isPriceAtSupport = latestPrice <= bollingerLower;
        boolean isPriceAtResistance = latestPrice >= bollingerUpper;

        // Entry logic
        if (isBullish5m && isBullish1h && !isPriceAtResistance && superTrendSignal5m > 0) {
            return new DecisionReason(Decision.LONG, "MACD bullish crossover on 5m and 1h, RSI below 60, and price above EMA15m.");
        } else if (isBearish5m && isBearish1h && !isPriceAtSupport && superTrendSignal5m < 0) {
            return new DecisionReason(Decision.SHORT, "MACD bearish crossover on 5m and 1h, RSI above 40, and price below EMA15m.");
        }

        // Exit logic
        if ((isPriceAtResistance && isBullish5m) || (isPriceAtSupport && isBearish5m) ||
                (macd5m < signalLine5m && isBullish5m) || (macd5m > signalLine5m && isBearish5m)) {
            return new DecisionReason(Decision.CLOSE, "Exit condition met based on MACD signal and Bollinger Bands thresholds.");
        }

        // Default hold decision
        return new DecisionReason(Decision.HOLD, "No clear entry or exit conditions met.");
    }

}
