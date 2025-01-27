package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.indicator.*;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.trader.StatData;
import com.github.istin.tradingaizer.trader.StatDealData;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.ArrayList;
import java.util.List;

public class IndicatorTestingApp {

    public static void main(String[] args) {
        // 1) Fetch data
        BinanceDataUtils.Result result = BinanceDataUtils.readBtcHistoricalData();
        List<? extends StatDealData> allKlineData = result.historicalData();

        int lastSize = 1000;
        if (allKlineData.size() < lastSize) {
            System.err.println("Not enough data to extract last 100 items.");
            return;
        }

        // 2) Sub-list for last 100 (or 1000, your choice)
        List<? extends StatDealData> lastItems = allKlineData.subList(
                Math.max(0, allKlineData.size() - lastSize),
                allKlineData.size()
        );


        // 3) Prepare a list of different indicators returning different result types.
        List<Indicator<?>> indicators = new ArrayList<>();
        indicators.add(new RSIIndicator(14));                // returns Double
        indicators.add(new SuperTrendIndicator(10, 3.0));    // returns Double
        indicators.add(new MACDIndicator(12, 26, 9));        // returns MACDIndicator.Result
        indicators.add( new ATRIndicator(14));
        indicators.add( new MovingAverageIndicator(50));
        indicators.add( new BollingerBandsIndicator(20, 2.0));
        indicators.add( new EMAIndicator(50));
        indicators.add( new OBVIndicator());


        // 4) For each indicator:
        for (Indicator<?> indicator : indicators) {
            // We'll fetch values from the full list & from the last sub-list
            Object fullValue = indicator.calculate((List<StatData>) castToStatData(allKlineData));
            Object lastValue = indicator.calculate((List<StatData>) castToStatData(lastItems));

            System.out.println("=== Indicator: " + indicator.toString() + " ===");
            System.out.println("Value with ALL data:  " + fullValue);
            System.out.println("Value with LAST data: " + lastValue);

            // Compare if both non-null
            if (fullValue != null && lastValue != null) {
                compareAndPrintDifference(fullValue, lastValue);
            } else {
                System.out.println("One or both returned null, cannot compare.\n");
            }
        }
    }

    /**
     * A helper method to cast List<KlineData> to List<? extends StatData> (if your
     * Indicator calculates on StatData).
     */
    @SuppressWarnings("unchecked")
    private static List<? extends StatData> castToStatData(List<? extends StatDealData> data) {
        // Some older code might require a raw cast. If your Indicator<T> uses
        // "List<? extends StatData>", you can do that more directly.
        return data;
    }

    /**
     * Compare two objects that might be different types (Double, MACDIndicator.Result, etc.).
     * If they're numeric, compute numeric difference; if they're MACD results, compare fields, etc.
     */
    private static void compareAndPrintDifference(Object fullValue, Object lastValue) {
        // A small tolerance for floating differences.
        double tolerance = 1e-9;

        // 1) If they're BOTH Numbers (e.g., Double from RSIIndicator, SuperTrendIndicator)
        if (fullValue instanceof Number && lastValue instanceof Number) {
            double v1 = ((Number) fullValue).doubleValue();
            double v2 = ((Number) lastValue).doubleValue();
            double diff = Math.abs(v1 - v2);

            if (diff < tolerance) {
                System.out.println("Same numeric result (within tolerance " + tolerance + "), diff=" + diff + "\n");
            } else {
                System.out.println("DIFFERENT numeric result. diff=" + diff + "\n");
            }
            return;
        }

        // 2) If they're BOTH MACDIndicator.Result
        if (fullValue instanceof MACDIndicator.Result && lastValue instanceof MACDIndicator.Result) {
            MACDIndicator.Result r1 = (MACDIndicator.Result) fullValue;
            MACDIndicator.Result r2 = (MACDIndicator.Result) lastValue;

            // Compare macd
            double macdDiff = Math.abs(r1.getMacd() - r2.getMacd());
            double signalDiff = Math.abs(r1.getSignalLine() - r2.getSignalLine());

            // Decide your own tolerance
            double maxAllowed = 1e-9;
            boolean sameMacd   = (macdDiff < maxAllowed);
            boolean sameSignal = (signalDiff < maxAllowed);

            System.out.println("MACD1=" + r1.getMacd() + ", Signal1=" + r1.getSignalLine());
            System.out.println("MACD2=" + r2.getMacd() + ", Signal2=" + r2.getSignalLine());
            System.out.println("MACD diff=" + macdDiff + ", Signal diff=" + signalDiff);

            if (sameMacd && sameSignal) {
                System.out.println("Same MACD results (within tolerance " + maxAllowed + ")\n");
            } else {
                System.out.println("DIFFERENT MACD results.\n");
            }
            return;
        }

        // 3) If both are BollingerBandsIndicator.Result
        if (fullValue instanceof BollingerBandsIndicator.Result && lastValue instanceof BollingerBandsIndicator.Result) {
            BollingerBandsIndicator.Result b1 = (BollingerBandsIndicator.Result) fullValue;
            BollingerBandsIndicator.Result b2 = (BollingerBandsIndicator.Result) lastValue;

            // Compare each band with a chosen tolerance
            double upperDiff  = Math.abs(b1.getUpperBand()   - b2.getUpperBand());
            double middleDiff = Math.abs(b1.getMiddleBand()  - b2.getMiddleBand());
            double lowerDiff  = Math.abs(b1.getLowerBand()   - b2.getLowerBand());

            double maxAllowed = 1e-9;
            boolean sameUpper  = upperDiff  < maxAllowed;
            boolean sameMiddle = middleDiff < maxAllowed;
            boolean sameLower  = lowerDiff  < maxAllowed;

            System.out.println("Bollinger 1 => upper=" + b1.getUpperBand()  +
                    ", middle=" + b1.getMiddleBand() +
                    ", lower="  + b1.getLowerBand());
            System.out.println("Bollinger 2 => upper=" + b2.getUpperBand()  +
                    ", middle=" + b2.getMiddleBand() +
                    ", lower="  + b2.getLowerBand());
            System.out.println("Diffs => upper=" + upperDiff +
                    ", middle=" + middleDiff +
                    ", lower=" + lowerDiff);

            if (sameUpper && sameMiddle && sameLower) {
                System.out.println("Same Bollinger values (within tolerance " + maxAllowed + ")\n");
            } else {
                System.out.println("DIFFERENT Bollinger values.\n");
            }
            return;
        }

        // 3) Otherwise, we don't know how to compare these result objects
        System.out.println("Can't compare these different result types:\n  fullValue=" + fullValue.getClass().getName() +
                "\n  lastValue=" + lastValue.getClass().getName() + "\n");
    }
}
