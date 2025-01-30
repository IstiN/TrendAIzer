package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Bollinger Bands indicator calculates the Upper, Middle, and Lower bands
 * for a given period and standard deviation multiplier.
 */
public class BollingerBandsIndicator extends Indicator<BollingerBandsIndicator.Result> {

    private final int period;
    private final double stdDevMultiplier;

    /**
     * @param period           the number of bars used for the calculation (commonly 20)
     * @param stdDevMultiplier the standard deviation multiplier (commonly 2.0)
     */
    public BollingerBandsIndicator(int period, double stdDevMultiplier) {
        this.period = period;
        this.stdDevMultiplier = stdDevMultiplier;
    }

    /**
     * Container for Bollinger Bands results: upper, middle, and lower bands.
     */
    @Data
    public static class Result implements Serializable {
        private static final long serialVersionUID = 1L;
        private double upperBand;
        private double middleBand;  // Typically the SMA
        private double lowerBand;
    }

    @Override
    public Result calculate(List<? extends StatData> historicalData) {
        if (historicalData == null || historicalData.size() < period) {
//            System.out.println(
//                    "Not enough data to calculate Bollinger Bands. Required: " + period +
//                            ", but got: " + (historicalData == null ? 0 : historicalData.size())
//            );
            return null;
        }

        // We'll calculate the Bollinger Bands for the most recent bar only
        // If you need a full series, adapt accordingly.

        // 1) Compute the Simple Moving Average (SMA) for the last 'period' bars
        double sum = 0.0;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            sum += historicalData.get(i).getClosePrice();
        }
        double sma = sum / period;

        // 2) Compute the standard deviation over the last 'period' bars
        double varianceSum = 0.0;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            double diff = historicalData.get(i).getClosePrice() - sma;
            varianceSum += (diff * diff);
        }
        double variance = varianceSum / period;
        double stdDev = Math.sqrt(variance);

        // 3) Create Bollinger Bands
        double upperBand = sma + (stdDevMultiplier * stdDev);
        double lowerBand = sma - (stdDevMultiplier * stdDev);

        // Return only the most recent set of bands
        Result bandsResult = new Result();
        bandsResult.setMiddleBand(sma);
        bandsResult.setUpperBand(upperBand);
        bandsResult.setLowerBand(lowerBand);

        return bandsResult;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (period=" + period + ", stdDevMultiplier=" + stdDevMultiplier + ")";
    }
}
