package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class OBVIndicator extends Indicator<Double> {

    @Override
    public Double calculate(List<StatData> historicalData) {
        if (historicalData == null || historicalData.size() < 2) {
            System.out.println("Not enough data to calculate OBV");
            return null;
        }

        double obv = 0.0;

        for (int i = 1; i < historicalData.size(); i++) {
            StatData previousData = historicalData.get(i - 1);
            StatData currentData = historicalData.get(i);

            double currentClosePrice = currentData.getClosePrice();
            double previousClosePrice = previousData.getClosePrice();
            double currentVolume = currentData.getVolume();

            if (currentClosePrice > previousClosePrice) {
                obv += currentVolume;
            } else if (currentClosePrice < previousClosePrice) {
                obv -= currentVolume;
            }
            // If prices are equal, OBV remains unchanged
        }

        return obv;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
