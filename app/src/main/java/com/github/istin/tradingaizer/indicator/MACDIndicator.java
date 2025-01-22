package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.ArrayList;
import java.util.List;

public class MACDIndicator extends Indicator {
    private int fastPeriod;
    private int slowPeriod;
    private int signalPeriod;

    public MACDIndicator(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    @Override
    public double calculate(List<KlineData> historicalData) {
        if (historicalData.size() < slowPeriod) {
            throw new IllegalArgumentException("Not enough data to calculate MACD.");
        }

        List<Double> closePrices = new ArrayList<>();
        for (KlineData data : historicalData) {
            closePrices.add(data.getClosePrice());
        }

        List<Double> fastEMAList = calculateEMAList(closePrices, fastPeriod);
        List<Double> slowEMAList = calculateEMAList(closePrices, slowPeriod);

        List<Double> macdValues = new ArrayList<>();
        for (int i = 0; i < slowEMAList.size(); i++) {
            macdValues.add(fastEMAList.get(i + (slowPeriod - fastPeriod)) - slowEMAList.get(i));
        }

        List<Double> signalLineList = calculateEMAList(macdValues, signalPeriod);

        double macd = macdValues.get(macdValues.size() - 1);
        double signalLine = signalLineList.get(signalLineList.size() - 1);

        return macd - signalLine;  // This is the MACD histogram value, you can modify the return value if needed
    }

    private List<Double> calculateEMAList(List<Double> prices, int period) {
        List<Double> emaList = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);

        double sma = 0;
        for (int i = 0; i < period; i++) {
            sma += prices.get(i);
        }
        sma /= period;
        emaList.add(sma);

        for (int i = period; i < prices.size(); i++) {
            double ema = ((prices.get(i) - emaList.get(emaList.size() - 1)) * multiplier) + emaList.get(emaList.size() - 1);
            emaList.add(ema);
        }

        return emaList;
    }
}
