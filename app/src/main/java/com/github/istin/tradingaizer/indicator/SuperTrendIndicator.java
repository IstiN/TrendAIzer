package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class SuperTrendIndicator extends Indicator<Double> {

    private final int atrPeriod;
    private final double multiplier;

    public SuperTrendIndicator(int atrPeriod, double multiplier) {
        this.atrPeriod = atrPeriod;
        this.multiplier = multiplier;
    }

    @Override
    public Double calculate(List<StatData> historicalData) {
        // We need at least (atrPeriod + 1) bars
        final int size = historicalData.size();
        if (size < atrPeriod + 1) {
            // Not enough data
            return null;
        }

        // 1) Compute TR (True Range) for each bar, but we only keep a rolling sum + rolling ATR
        // We want a "smoothed" ATR matching your original logic:
        //   - Sum the first 'period' TRs => initialATR
        //   - Then for each subsequent TR, do: ATR = ((prevATR*(period-1)) + currentTR) / period
        double[] trValues = new double[size];
        // TR starts from index 1 onward
        for (int i = 1; i < size; i++) {
            double high = historicalData.get(i).getHighPrice();
            double low  = historicalData.get(i).getLowPrice();
            double prevClose = historicalData.get(i - 1).getClosePrice();
            double range1 = high - low;
            double range2 = Math.abs(high - prevClose);
            double range3 = Math.abs(low - prevClose);
            trValues[i] = Math.max(range1, Math.max(range2, range3));
        }

        // Initial ATR = average of the first 'atrPeriod' TRs, i.e. from i=1..atrPeriod
        double sumTR = 0.0;
        for (int i = 1; i <= atrPeriod; i++) {
            sumTR += trValues[i];
        }
        double atr = sumTR / atrPeriod;

        // Rolling compute for bars > atrPeriod
        for (int i = atrPeriod + 1; i < size; i++) {
            atr = ((atr * (atrPeriod - 1)) + trValues[i]) / atrPeriod;
        }

        // 2) For the very last bar:
        //    basicUpperBand = midpoint + multiplier * finalATR
        //    basicLowerBand = midpoint - multiplier * finalATR
        StatData lastBar = historicalData.get(size - 1);
        double midPoint = (lastBar.getHighPrice() + lastBar.getLowPrice()) / 2.0;
        double basicUpperBand = midPoint + (multiplier * atr);
        double basicLowerBand = midPoint - (multiplier * atr);

        // 3) Simplified SuperTrend signal:
        //    return 1 if last close > lower band, -1 otherwise
        double lastClose = lastBar.getClosePrice();
        return (lastClose > basicLowerBand) ? 1.0 : -1.0;
    }

    @Override
    public String toString() {
        return this.getClass() + " " + atrPeriod + " " + multiplier;
    }
}
