package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class MovingAverageIndicator extends Indicator<Double> {
    private int period;

    public MovingAverageIndicator(int period) {
        this.period = period;
    }

    @Override
    public Double calculate(List<? extends StatData> historicalData) {
        int size = historicalData.size();
        if (size < period) {
//            System.out.println("Not enough data to calculate Moving Average");
            return null;
        }

        double sum = 0.0;
        for (int i = size - period; i < size; i++) {
            sum += historicalData.get(i).getClosePrice();
        }

        return sum / period;
    }

    @Override
    public String toString() {
        return this.getClass() + " " + period;
    }
}