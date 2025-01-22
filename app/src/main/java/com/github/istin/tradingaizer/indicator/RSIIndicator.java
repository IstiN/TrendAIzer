package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class RSIIndicator extends Indicator {
    private int period;

    public RSIIndicator(int period) {
        this.period = period;
    }

    @Override
    public double calculate(List<KlineData> historicalData) {
        if (historicalData.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data to calculate RSI");
        }

        double gain = 0, loss = 0;

        for (int i = 1; i <= period; i++) {
            double change = historicalData.get(i).getClosePrice() - historicalData.get(i - 1).getClosePrice();
            if (change > 0) {
                gain += change;
            } else {
                loss += Math.abs(change);
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        for (int i = period + 1; i < historicalData.size(); i++) {
            double change = historicalData.get(i).getClosePrice() - historicalData.get(i - 1).getClosePrice();
            if (change > 0) {
                avgGain = ((avgGain * (period - 1)) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgLoss = ((avgLoss * (period - 1)) + Math.abs(change)) / period;
                avgGain = (avgGain * (period - 1)) / period;
            }
        }

        if (avgLoss == 0) return 100; // Prevent division by zero
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
