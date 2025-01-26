package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RSIIndicatorTest {

    /**
     * A simple concrete class for testing. We store open, high, low, close, volume
     * but only expose the methods required by StatData.
     */
    private static class MockStatData implements StatData {
        private final double high;
        private final double low;
        private final double close;
        private final double volume;

        // We'll keep 'open' for demonstration, but StatData doesn't require it:
        private final double open;

        MockStatData(double open, double high, double low, double close, double volume) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        @Override
        public double getHighPrice() {
            return high;
        }

        @Override
        public double getLowPrice() {
            return low;
        }

        @Override
        public double getClosePrice() {
            return close;
        }

        @Override
        public double getVolume() {
            return volume;
        }
    }

    @Test
    void testRSIInsufficientData() {
        RSIIndicator rsiIndicator = new RSIIndicator(14);

        // We need at least (period+1)=15 bars => give only 10
        List<StatData> insufficientData = generateStatDataList(10, 100.0, 1.0);
        Double rsiValue = rsiIndicator.calculate(insufficientData);

        Assertions.assertNull(rsiValue, "RSI should be null for insufficient data");
    }

    @Test
    void testRSISufficientData() {
        RSIIndicator rsiIndicator = new RSIIndicator(14);

        // Provide 20 data points => enough for period+1=15
        List<StatData> sufficientData = generateStatDataList(20, 100.0, 1.0);
        Double rsiValue = rsiIndicator.calculate(sufficientData);

        Assertions.assertNotNull(rsiValue, "RSI should not be null if data is sufficient");
        Assertions.assertTrue(rsiValue >= 0 && rsiValue <= 100,
                "RSI value should be between 0 and 100");
    }

    @Test
    void testKnownRsiValueExample() {
        // A well-known RSI example: 15 bars for a 14-period RSI
        // Indexing bars from 0..14 for clarity:
        // Source: "Classic" RSI demonstration data.
        // The expected result is around 70.5 after the 14th period.

        RSIIndicator rsiIndicator = new RSIIndicator(14);

        // Exactly 15 data points => enough to compute an RSI(14).
        // (We only need close prices for RSI, but we'll fill open/high/low anyway.)
        // day 0: close=44.34
        // day 1: close=44.09
        // day 2: close=44.15
        // day 3: close=43.61
        // day 4: close=44.33
        // day 5: close=44.83
        // day 6: close=45.10
        // day 7: close=45.42
        // day 8: close=45.84
        // day 9: close=46.08
        // day10: close=45.89
        // day11: close=46.03
        // day12: close=45.61
        // day13: close=46.28
        // day14: close=46.28  (no change on the last day)
        List<StatData> data = new ArrayList<>();
        data.add(new MockStatData(0, 0, 0, 44.34, 0));
        data.add(new MockStatData(0, 0, 0, 44.09, 0));
        data.add(new MockStatData(0, 0, 0, 44.15, 0));
        data.add(new MockStatData(0, 0, 0, 43.61, 0));
        data.add(new MockStatData(0, 0, 0, 44.33, 0));
        data.add(new MockStatData(0, 0, 0, 44.83, 0));
        data.add(new MockStatData(0, 0, 0, 45.10, 0));
        data.add(new MockStatData(0, 0, 0, 45.42, 0));
        data.add(new MockStatData(0, 0, 0, 45.84, 0));
        data.add(new MockStatData(0, 0, 0, 46.08, 0));
        data.add(new MockStatData(0, 0, 0, 45.89, 0));
        data.add(new MockStatData(0, 0, 0, 46.03, 0));
        data.add(new MockStatData(0, 0, 0, 45.61, 0));
        data.add(new MockStatData(0, 0, 0, 46.28, 0));
        data.add(new MockStatData(0, 0, 0, 46.28, 0));

        Double rsiValue = rsiIndicator.calculate(data);
        Assertions.assertNotNull(rsiValue, "RSI should not be null with 15 data points for RSI(14).");

        // The "classic" result for this sequence is about 70.5
        // We allow for small rounding differences.
        double expectedRsi = 70.5;
        double delta = 0.1; // tolerance
        Assertions.assertEquals(expectedRsi, rsiValue, delta,
                "RSI should match the known reference ~70.5 ± 0.1");
    }

    @Test
    void testRSIKnownScenario() {
        RSIIndicator rsiIndicator = new RSIIndicator(5);

        // Provide a small set of known data.
        // period=5 => we need at least 6 bars. We have 10 here.
        List<StatData> knownData = new ArrayList<>();
        knownData.add(new MockStatData(100, 100, 100, 100, 0));
        knownData.add(new MockStatData(100, 101, 99, 101, 0));
        knownData.add(new MockStatData(101, 103, 100, 102, 0));
        knownData.add(new MockStatData(102, 105, 101, 104, 0));
        knownData.add(new MockStatData(104, 105, 103, 105, 0));
        knownData.add(new MockStatData(105, 106, 104, 106, 0));
        knownData.add(new MockStatData(106, 107, 105, 107, 0));
        knownData.add(new MockStatData(107, 108, 106, 106, 0));
        knownData.add(new MockStatData(106, 107, 105, 105, 0));
        knownData.add(new MockStatData(105, 106, 104, 105, 0));

        Double rsiValue = rsiIndicator.calculate(knownData);
        Assertions.assertNotNull(rsiValue, "RSI should be calculated");
        System.out.println("Computed RSI: " + rsiValue);

        // Typical RSI range check
        Assertions.assertTrue(rsiValue >= 0 && rsiValue <= 100,
                "RSI should be in [0..100]");
    }

    /**
     * Generate a list of data with a simple pattern:
     * - open=high=low=close, increment by 'increment' each bar
     */
    private List<StatData> generateStatDataList(int count, double start, double increment) {
        List<StatData> dataList = new ArrayList<>();
        double currentPrice = start;
        for (int i = 0; i < count; i++) {
            dataList.add(new MockStatData(
                    currentPrice,   // open
                    currentPrice,   // high
                    currentPrice,   // low
                    currentPrice,   // close
                    0               // volume
            ));
            currentPrice += increment;
        }
        return dataList;
    }
}
