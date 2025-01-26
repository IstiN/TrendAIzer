package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.ATRIndicator;
import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

/**
 * Refined AdaptiveMultiTimeframeStrategy
 * - Wider stop-loss & take-profit multipliers to reduce whipsaws
 * - Stronger MACD & RSI thresholds for clearer signals
 * - Exits on MACD reversal vs. higher timeframe trend
 */
public class AdaptiveMultiTimeframeStrategy extends Strategy {

    private static final double STOP_LOSS_MULTIPLIER   = 3.0;
    private static final double TAKE_PROFIT_MULTIPLIER = 5.0;
    private static final double MACD_ENTRY_THRESHOLD   = 0.2;
    private static final double MACD_EXIT_THRESHOLD    = 0.05;
    private static final double RSI_BULLISH_5M         = 65.0;
    private static final double RSI_BEARISH_5M         = 35.0;
    private static final double RSI_UPTREND_1H         = 60.0;
    private static final double RSI_DOWNTREND_1H       = 40.0;

    public AdaptiveMultiTimeframeStrategy(String cacheId, ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        // Retrieve 5m and 1h data
        List<? extends StatData> data5m = getData(historicalData, Timeframe.M5);
        List<? extends StatData> data1h = getData(historicalData, Timeframe.H1);

        if (data5m.isEmpty() || data1h.isEmpty()) {
            return new DecisionReason(Decision.HOLD, "Insufficient data");
        }

        // Indicators on 5m
        MACDIndicator macd5m = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi5m   = new RSIIndicator(14);
        ATRIndicator atr5m   = new ATRIndicator(14);

        // Indicators on 1h
        MACDIndicator macd1h = new MACDIndicator(12, 26, 9);
        RSIIndicator rsi1h   = new RSIIndicator(14);

        // Calculate indicator values
        MACDIndicator.Result macdVal5m = calcOrFromCache(macd5m, data5m, Timeframe.M5);
        Double rsiVal5m                = calcOrFromCache(rsi5m, data5m, Timeframe.M5);
        Double atrVal5m                = calcOrFromCache(atr5m, data5m, Timeframe.M5);

        MACDIndicator.Result macdVal1h = calcOrFromCache(macd1h, data1h, Timeframe.H1);
        Double rsiVal1h                = calcOrFromCache(rsi1h, data1h, Timeframe.H1);

        if (macdVal5m == null || rsiVal5m == null || atrVal5m == null ||
                macdVal1h == null || rsiVal1h == null) {
            return new DecisionReason(Decision.HOLD, "Indicator data unavailable");
        }

        // Latest close price
        StatData latest = historicalData.get(historicalData.size() - 1);
        double latestPrice = latest.getClosePrice();

        // Higher timeframe (1h) trend
        boolean uptrend1h   = (macdVal1h.getMacd() > macdVal1h.getSignalLine()) && (rsiVal1h > RSI_UPTREND_1H);
        boolean downtrend1h = (macdVal1h.getMacd() < macdVal1h.getSignalLine()) && (rsiVal1h < RSI_DOWNTREND_1H);

        // ATR-based distances
        double stopDistance = atrVal5m * STOP_LOSS_MULTIPLIER;
        double tpDistance   = atrVal5m * TAKE_PROFIT_MULTIPLIER;

        // MACD difference (5m)
        double macdDiff5m = macdVal5m.getMacd() - macdVal5m.getSignalLine();

        // Entry conditions (5m + 1h)
        boolean bullish5m = (macdDiff5m > MACD_ENTRY_THRESHOLD) && (rsiVal5m >= RSI_BULLISH_5M);
        boolean bearish5m = (macdDiff5m < -MACD_ENTRY_THRESHOLD) && (rsiVal5m <= RSI_BEARISH_5M);

        // Long entry
        if (uptrend1h && bullish5m) {
            double sl = latestPrice - stopDistance;
            double tp = latestPrice + tpDistance;
            return new DecisionReason(Decision.LONG, "Uptrend + bullish 5m", sl, tp);
        }

        // Short entry
        if (downtrend1h && bearish5m) {
            double sl = latestPrice + stopDistance;
            double tp = latestPrice - tpDistance;
            return new DecisionReason(Decision.SHORT, "Downtrend + bearish 5m", sl, tp);
        }

        // Exit logic (5m MACD crossing in the opposite direction of the 1h trend)
        boolean macdUp5m   = (macdDiff5m >  MACD_EXIT_THRESHOLD);
        boolean macdDown5m = (macdDiff5m < -MACD_EXIT_THRESHOLD);

        if ((uptrend1h && macdDown5m) || (downtrend1h && macdUp5m)) {
            return new DecisionReason(Decision.CLOSE, "Exit: MACD reversal vs trend");
        }

        // Otherwise, hold
        return new DecisionReason(Decision.HOLD, "No clear signal");
    }
}
