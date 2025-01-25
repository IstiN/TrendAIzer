package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;
import lombok.Data;

import java.util.List;

public class BollingerBandsIndicator extends Indicator<BollingerBandsIndicator.Result> {
    private int period;
    private double multiplier;

    @Data
    public static class Result {
        private double upperBand;
        private double lowerBand;
    }

    public BollingerBandsIndicator(int period, double multiplier) {
        this.period = period;
        this.multiplier = multiplier;
    }

    private Double getUpperBand(List<StatData> historicalData) {
        Double sma = calculateSMA(historicalData);
        if (sma == null) {
            return null;
        }
        Double stdDev = calculateStandardDeviation(historicalData);
        if (stdDev == null) {
            return null;
        }
        return sma + (multiplier * stdDev);
    }

    private Double getLowerBand(List<StatData> historicalData) {
        Double sma = calculateSMA(historicalData);
        if (sma == null) {
            return null;
        }
        Double stdDev = calculateStandardDeviation(historicalData);
        if (stdDev == null) {
            return null;
        }
        return sma - (multiplier * stdDev);
    }

    private Double calculateSMA(List<StatData> historicalData) {
        if (historicalData.size() < period) {
            System.out.println("Not enough data to calculate SMA for Bollinger Bands");
            return null;
        }

        double sum = 0;
        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            sum += historicalData.get(i).getClosePrice();
        }
        return sum / period;
    }

    private Double calculateStandardDeviation(List<StatData> historicalData) {
        if (historicalData.size() < period) {
            System.out.println("Not enough data to calculate standard deviation for Bollinger Bands");
            return null;
        }

        Double sma = calculateSMA(historicalData);
        if (sma == null) {
            return null;
        }
        double sumSquaredDifferences = 0;

        for (int i = historicalData.size() - period; i < historicalData.size(); i++) {
            double price = historicalData.get(i).getClosePrice();
            sumSquaredDifferences += Math.pow(price - sma, 2);
        }

        return Math.sqrt(sumSquaredDifferences / period);
    }

    @Override
    public BollingerBandsIndicator.Result calculate(List<StatData> historicalData) {
        Result result = new Result();
        Double upperBand = getUpperBand(historicalData);
        if (upperBand == null) {
            return null;
        }
        result.setUpperBand(upperBand);
        Double lowerBand = getLowerBand(historicalData);
        if (lowerBand == null) {
            return null;
        }
        result.setLowerBand(lowerBand);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass() + " " + period + " " + multiplier;
    }
}
