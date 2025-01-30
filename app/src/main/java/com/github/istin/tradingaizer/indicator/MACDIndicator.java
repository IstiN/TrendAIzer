package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

public class MACDIndicator extends Indicator<MACDIndicator.Result> {
    private int fastPeriod;
    private int slowPeriod;
    private int signalPeriod;

    @Data
    public static class Result implements Serializable {
        private static final long serialVersionUID = 1L;
        private double macd;
        private double signalLine;
    }

    public MACDIndicator(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    @Override
    public Result calculate(List<? extends StatData> historicalData) {
        if (historicalData.size() < slowPeriod) {
//            System.out.println(
//                    "Not enough data to calculate MACD. Required: " + slowPeriod + ", but got: " + historicalData.size()
//            );
            return null;
        }

        double[] macdValues = new double[historicalData.size() - slowPeriod + 1];
        double[] signalLineValues = new double[macdValues.length];

        calculateEMAs(historicalData, macdValues, signalLineValues);

        double macd = macdValues[macdValues.length - 1];
        double signalLine = signalLineValues[signalLineValues.length - 1];

        Result result = new Result();
        result.setMacd(macd - signalLine);
        result.setSignalLine(signalLine);
        return result;
    }

    private void calculateEMAs(List<? extends StatData> historicalData, double[] macdValues, double[] signalLineValues) {
        int size = historicalData.size();
        double fastMultiplier = 2.0 / (fastPeriod + 1);
        double slowMultiplier = 2.0 / (slowPeriod + 1);
        double signalMultiplier = 2.0 / (signalPeriod + 1);

        double fastEMA = 0;
        double slowEMA = 0;

        // Calculate initial SMAs for fast and slow periods
        for (int i = 0; i < slowPeriod; i++) {
            StatData currentData = historicalData.get(i);
            if (i < fastPeriod) {
                fastEMA += currentData.getClosePrice();
            }
            slowEMA += currentData.getClosePrice();
        }

        fastEMA /= fastPeriod;
        slowEMA /= slowPeriod;

        // Initialize the first MACD value
        macdValues[0] = fastEMA - slowEMA;

        // Calculate EMA and MACD values
        for (int i = slowPeriod; i < size; i++) {
            StatData currentData = historicalData.get(i);
            double closePrice = currentData.getClosePrice();

            fastEMA = ((closePrice - fastEMA) * fastMultiplier) + fastEMA;
            slowEMA = ((closePrice - slowEMA) * slowMultiplier) + slowEMA;

            int macdIndex = i - slowPeriod + 1;
            macdValues[macdIndex] = fastEMA - slowEMA;

            // Calculate Signal Line EMA iteratively
            if (macdIndex == 0) {
                signalLineValues[0] = macdValues[0];
            } else {
                signalLineValues[macdIndex] = ((macdValues[macdIndex] - signalLineValues[macdIndex - 1]) * signalMultiplier) + signalLineValues[macdIndex - 1];
            }
        }
    }

    @Override
    public String toString() {
        return this.getClass() + " " + fastPeriod + " " + slowPeriod + " " + signalPeriod;
    }
}
