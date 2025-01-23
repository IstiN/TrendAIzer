package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public class MovingAverageIndicator extends Indicator {
    private int period;

    public MovingAverageIndicator(int period) {
        this.period = period;
    }

    @Override
    public double calculate(List<KlineData> historicalData) {
        if (historicalData.size() < period) {
            throw new IllegalArgumentException("Not enough data to calculate Moving Average");
        }

        double sum = 0.0;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            sum += historicalData.get(i).getClosePrice();
        }

        return sum / period;
    }
}