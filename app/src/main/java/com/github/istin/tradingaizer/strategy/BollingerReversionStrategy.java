package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

/**
 * A more lenient Bollinger Reversion Strategy:
 * 1. Uses Bollinger Bands with relaxed multipliers for upper/lower band detection.
 * 2. Allows looser RSI thresholds so trades are more likely to open.
 * 3. Provides a simple exit condition when price returns to the Bollinger mid-band.
 */
public class BollingerReversionStrategy extends Strategy {

    private static final int    BOLLINGER_PERIOD  = 20;
    private static final double BOLLINGER_STD_DEV = 2.0;
    private static final int    EMA_PERIOD        = 50;
    private static final int    RSI_PERIOD        = 14;

    // RSI thresholds are a bit looser to generate signals more readily
    private static final double RSI_OVERBOUGHT    = 65.0;
    private static final double RSI_OVERSOLD      = 35.0;

    // Relaxed band conditions
    private static final double UPPER_BAND_FACTOR = 0.95; // Trigger earlier near the upper band
    private static final double LOWER_BAND_FACTOR = 1.05; // Trigger earlier near the lower band

    // ATR-based stop-loss multiplier
    private static final double STOP_LOSS_ATR_MULTIPLIER = 2.0;

    public BollingerReversionStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // We'll work primarily with 15-minute data
        List<? extends StatData> data15m = getData(historicalData, Timeframe.M15);
        if (data15m.isEmpty()) {
            return new DecisionReason(Decision.HOLD, "No data for 15m timeframe");
        }

        BollingerBandsIndicator bbIndicator = new BollingerBandsIndicator(BOLLINGER_PERIOD, BOLLINGER_STD_DEV);
        EMAIndicator emaIndicator          = new EMAIndicator(EMA_PERIOD);
        RSIIndicator rsiIndicator          = new RSIIndicator(RSI_PERIOD);
        ATRIndicator atrIndicator          = new ATRIndicator(14);

        BollingerBandsIndicator.Result bbValues = calcOrFromCache(bbIndicator, data15m, Timeframe.M15);
        Double emaValue   = calcOrFromCache(emaIndicator, data15m, Timeframe.M15);
        Double rsiValue   = calcOrFromCache(rsiIndicator, data15m, Timeframe.M15);
        Double atrValue   = calcOrFromCache(atrIndicator, data15m, Timeframe.M15);

        if (bbValues == null || emaValue == null || rsiValue == null || atrValue == null) {
            return new DecisionReason(Decision.HOLD, "Insufficient indicator data");
        }

        StatData latestBar = data15m.get(data15m.size() - 1);
        double latestPrice = latestBar.getClosePrice();

        double upperBand  = bbValues.getUpperBand();
        double middleBand = bbValues.getMiddleBand();
        double lowerBand  = bbValues.getLowerBand();

        // ATR-based stop-loss distance
        double stopLossDist = atrValue * STOP_LOSS_ATR_MULTIPLIER;

        // Price near upper/lower band triggers
        boolean nearUpperBand = latestPrice >= (upperBand * UPPER_BAND_FACTOR);
        boolean nearLowerBand = latestPrice <= (lowerBand * LOWER_BAND_FACTOR);

        // Trend check with EMA
        boolean priceAboveEMA = latestPrice > emaValue;
        boolean priceBelowEMA = latestPrice < emaValue;

        // Potential Long Entry
        // If near lower band, oversold RSI, and price is not drastically below EMA
        if (nearLowerBand && rsiValue <= RSI_OVERSOLD && priceAboveEMA) {
            double sl = latestPrice - stopLossDist;
            double tp = latestPrice + stopLossDist * 2.0; // 1:2 Risk:Reward
            return new DecisionReason(Decision.LONG,
                    "Bollinger near lower band + RSI oversold + price above EMA => LONG",
                    sl, tp
            );
        }

        // Potential Short Entry
        // If near upper band, overbought RSI, and price is not drastically above EMA
        if (nearUpperBand && rsiValue >= RSI_OVERBOUGHT && priceBelowEMA) {
            double sl = latestPrice + stopLossDist;
            double tp = latestPrice - stopLossDist * 2.0;
            return new DecisionReason(Decision.SHORT,
                    "Bollinger near upper band + RSI overbought + price below EMA => SHORT",
                    sl, tp
            );
        }

        // Simple exit logic: if price crosses the Bollinger mid-band
        // or RSI goes neutral, close the position
        if ((latestPrice > middleBand * 0.98 && latestPrice < middleBand * 1.02) &&
                rsiValue > 45 && rsiValue < 55) {
            return new DecisionReason(Decision.CLOSE, "Price crossed Bollinger mid-band => exit position");
        }

        // Otherwise HOLD
        return new DecisionReason(Decision.HOLD, "No reversion signal triggered");
    }
}
