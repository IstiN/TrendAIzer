package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.ArrayList;
import java.util.List;

public class SuperTrendIndicator extends Indicator<Double> {
    private int atrPeriod;
    private double multiplier;

    public SuperTrendIndicator(int atrPeriod, double multiplier) {
        this.atrPeriod = atrPeriod;
        this.multiplier = multiplier;
    }

    @Override
    public Double calculate(List<StatData> historicalData) {
        if (historicalData.size() < atrPeriod + 1) {
            System.out.println("Not enough data to calculate SuperTrend.");
            return null;
        }

        List<Double> high = new ArrayList<>();
        List<Double> low = new ArrayList<>();
        List<Double> close = new ArrayList<>();
        for (StatData data : historicalData) {
            high.add(data.getHighPrice());
            low.add(data.getLowPrice());
            close.add(data.getClosePrice());
        }

        // Calculate ATR
        List<Double> atr = calculateATR(high, low, close, atrPeriod);

        // Calculate SuperTrend Bands
        List<Double> upperBand = new ArrayList<>();
        List<Double> lowerBand = new ArrayList<>();
        for (int i = atrPeriod; i < high.size(); i++) {
            double basicUpperBand = (high.get(i) + low.get(i)) / 2 + (multiplier * atr.get(i - atrPeriod));
            double basicLowerBand = (high.get(i) + low.get(i)) / 2 - (multiplier * atr.get(i - atrPeriod));
            upperBand.add(basicUpperBand);
            lowerBand.add(basicLowerBand);
        }

        // Simplified SuperTrend calculation for decision-making
        return historicalData.get(historicalData.size() - 1).getClosePrice() > lowerBand.get(lowerBand.size() - 1) ? 1d : -1d;
    }

    private List<Double> calculateATR(List<Double> high, List<Double> low, List<Double> close, int period) {
        List<Double> atr = new ArrayList<>();

        for (int i = 1; i < high.size(); i++) {
            double tr = Math.max(high.get(i) - low.get(i),
                    Math.max(Math.abs(high.get(i) - close.get(i - 1)),
                            Math.abs(low.get(i) - close.get(i - 1))));
            atr.add(tr);
        }

        List<Double> atrSmoothed = new ArrayList<>();
        double initialATR = 0;
        for (int i = 0; i < period; i++) {
            initialATR += atr.get(i);
        }
        initialATR /= period;
        atrSmoothed.add(initialATR);

        for (int i = period; i < atr.size(); i++) {
            double currentATR = ((atrSmoothed.get(atrSmoothed.size() - 1) * (period - 1)) + atr.get(i)) / period;
            atrSmoothed.add(currentATR);
        }

        return atrSmoothed;
    }

    @Override
    public String toString() {
        return this.getClass() + " " + atrPeriod + " " + multiplier;
    }
}