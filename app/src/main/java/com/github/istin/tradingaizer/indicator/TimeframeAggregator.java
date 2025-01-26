package com.github.istin.tradingaizer.indicator;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.ArrayList;
import java.util.List;

public class TimeframeAggregator {

    public static List<StatData> convertToTimeframe(List<? extends StatData> data, Timeframe timeframe) {
        List<StatData> aggregatedData = new ArrayList<>();
        int interval = timeframe.getMinutes(); // Get the interval in minutes

        for (int i = 0; i < data.size(); i += interval) {
            // Ensure we have enough data for the interval
            int endIndex = Math.min(i + interval, data.size());
            List<? extends StatData> group = data.subList(i, endIndex);

            double high = group.stream()
                    .mapToDouble(StatData::getHighPrice)
                    .max()
                    .orElse(0);

            double low = group.stream()
                    .mapToDouble(StatData::getLowPrice)
                    .min()
                    .orElse(0);

            double close = group.get(group.size() - 1).getClosePrice();

            double volume = group.stream()
                    .mapToDouble(StatData::getVolume)
                    .sum();

            // Create a new `StatData` implementation for the aggregated candle
            StatData aggregatedCandle = new AggregatedStatData(high, low, close, volume);
            aggregatedData.add(aggregatedCandle);
        }

        return aggregatedData;
    }

    /**
     * A basic implementation of the StatData interface for aggregated candles.
     */
    public static class AggregatedStatData implements StatData {
        private final double highPrice;
        private final double lowPrice;
        private final double closePrice;
        private final double volume;

        public AggregatedStatData(double highPrice, double lowPrice, double closePrice, double volume) {
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.closePrice = closePrice;
            this.volume = volume;
        }

        @Override
        public double getHighPrice() {
            return highPrice;
        }

        @Override
        public double getLowPrice() {
            return lowPrice;
        }

        @Override
        public double getClosePrice() {
            return closePrice;
        }

        @Override
        public double getVolume() {
            return volume;
        }
    }
}
