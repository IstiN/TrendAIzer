package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

// Average True Range (ATR) Indicator Implementation
public class ATRIndicator extends Indicator {
    private int period;

    public ATRIndicator(int period) {
        this.period = period;
    }

    @Override
    public double calculate(List<KlineData> historicalData) {
        if (historicalData.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data to calculate ATR");
        }

        double atr = 0.0;
        for (int i = 1; i <= period; i++) {
            KlineData current = historicalData.get(i);
            KlineData previous = historicalData.get(i - 1);

            double highLow = current.getHighPrice() - current.getLowPrice();
            double highClose = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lowClose = Math.abs(current.getLowPrice() - previous.getClosePrice());

            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            atr += trueRange;
        }

        atr /= period;

        for (int i = period + 1; i < historicalData.size(); i++) {
            KlineData current = historicalData.get(i);
            KlineData previous = historicalData.get(i - 1);

            double highLow = current.getHighPrice() - current.getLowPrice();
            double highClose = Math.abs(current.getHighPrice() - previous.getClosePrice());
            double lowClose = Math.abs(current.getLowPrice() - previous.getClosePrice());

            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            atr = ((atr * (period - 1)) + trueRange) / period;
        }

        return atr;
    }
}