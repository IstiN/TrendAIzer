package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class RSIIndicator extends Indicator<Double> {
    private int period;

    public RSIIndicator(int period) {
        this.period = period;
    }

    @Override
    public Double calculate(List<? extends StatData> historicalData) {
        if (historicalData.size() < period + 1) {
            //System.out.println("Not enough data to calculate RSI");
            return null;
        }

        double gain = 0, loss = 0;

        // Calculate initial gains and losses
        for (int i = 1; i <= period; i++) {
            StatData prevData = historicalData.get(i - 1);
            StatData currentData = historicalData.get(i);
            double change = currentData.getClosePrice() - prevData.getClosePrice();
            if (change > 0) {
                gain += change;
            } else {
                loss += Math.abs(change);
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        // Calculate smoothed average gain and loss
        for (int i = period + 1; i < historicalData.size(); i++) {
            StatData prevData = historicalData.get(i - 1);
            StatData currentData = historicalData.get(i);
            double change = currentData.getClosePrice() - prevData.getClosePrice();
            if (change > 0) {
                avgGain = ((avgGain * (period - 1)) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgLoss = ((avgLoss * (period - 1)) + Math.abs(change)) / period;
                avgGain = (avgGain * (period - 1)) / period;
            }
        }

        if (avgLoss == 0) return 100d; // Prevent division by zero
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public String toString() {
        return this.getClass() + " " + period;
    }
}
