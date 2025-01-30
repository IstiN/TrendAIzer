package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

// Average True Range (ATR) Indicator Implementation
public class ATRIndicator extends Indicator<Double> {
    private int period;

    public ATRIndicator(int period) {
        this.period = period;
    }

    @Override
    public Double calculate(List<? extends StatData> historicalData) {
        if (historicalData.size() < period + 1) {
//            System.out.println("Not enough data to calculate ATR");
            return null;
        }

        double atr = 0.0;
        for (int i = 1; i <= period; i++) {
            StatData current = historicalData.get(i);
            StatData previous = historicalData.get(i - 1);

            double highLow = current.getHighPrice() - current.getLowPrice();
            double highClose = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lowClose = Math.abs(current.getLowPrice() - previous.getClosePrice());

            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            atr += trueRange;
        }

        atr /= period;

        for (int i = period + 1; i < historicalData.size(); i++) {
            StatData current = historicalData.get(i);
            StatData previous = historicalData.get(i - 1);

            double highLow = current.getHighPrice() - current.getLowPrice();
            double highClose = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lowClose = Math.abs(current.getLowPrice() - previous.getClosePrice());

            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            atr = ((atr * (period - 1)) + trueRange) / period;
        }

        return atr;
    }
    @Override
    public String toString() {
        return this.getClass() + " " + period;
    }
}