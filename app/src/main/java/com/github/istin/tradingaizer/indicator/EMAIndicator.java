package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

/**
 * EMAIndicator calculates the Exponential Moving Average (EMA) over a given period.
 */
public class EMAIndicator extends Indicator<Double> {

    private final int period;

    public EMAIndicator(int period) {
        this.period = period;
    }

    @Override
    public Double calculate(List<? extends StatData> historicalData) {
        // Ensure enough data points
        if (historicalData == null || historicalData.size() < period) {
//            System.out.println("Not enough data to calculate EMA. Required: " + period);
            return null;
        }

        // Initial sum for the first 'period' bars to get the initial SMA
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            sum += historicalData.get(i).getClosePrice();
        }
        double sma = sum / period;

        // Multiplier for weight
        double multiplier = 2.0 / (period + 1);

        // Start from the first EMA value
        double ema = sma;

        // Calculate EMA for the rest of bars
        for (int i = period; i < historicalData.size(); i++) {
            double closePrice = historicalData.get(i).getClosePrice();
            ema = ((closePrice - ema) * multiplier) + ema;
        }

        // Return the last calculated EMA
        return ema;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " period=" + period;
    }
}
