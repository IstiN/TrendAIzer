package com.github.istin.tradingaizer.chart;

import com.github.istin.tradingaizer.indicator.Indicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.indicator.TimeframeAggregator;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChartDataProvider {

    private final List<? extends StatData> data1m;
    private final List<StatData> data1h;
    private final List<StatData> data15m;
    private final List<StatData> data5m;
    private final Map<String, Map<Timeframe, List<?>>> indicatorCache = new ConcurrentHashMap<>();

    public ChartDataProvider(List<? extends StatData> statData1MinuttimeFrame) {
        this.data1m = statData1MinuttimeFrame;
        this.data5m = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.M5);
        this.data15m = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.M15);
        this.data1h = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.H1);
    }

    public List<? extends StatData> getData(List<? extends StatData> historicalData, Timeframe timeframe) {
        switch (timeframe) {
            case M1:
                return data1m;
            case M5:
                return data5m;
            case M15:
                return data15m;
            case H1:
                return data1h;
            default:
                throw new IllegalStateException("Unexpected value: " + timeframe);
        }
    }

    public <Result> Result calculateIndicator(Indicator<Result> indicator, List<? extends StatData> historicalData, Timeframe timeframe) {
        String indicatorKey = indicator.toString();

        // Ensure thread-safe access to cache for the given indicator
        indicatorCache.putIfAbsent(indicatorKey, new ConcurrentHashMap<>());
        Map<Timeframe, List<?>> timeframeCache = indicatorCache.get(indicatorKey);

        // Check if the values are already cached for this timeframe
        if (timeframeCache.containsKey(timeframe)) {
            return ((List<Result>) timeframeCache.get(timeframe)).get(historicalData.size() - 1);
        }

        List<? extends StatData> data;
        switch (timeframe) {
            case M1:
                data = data1m;
                break;
            case M5:
                data = data5m;
                break;
            case M15:
                data = data15m;
                break;
            case H1:
                data = data1h;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + timeframe);
        }
        List<StatData> timeline = new ArrayList<>();
        List<Result> indicators = new ArrayList<>();
        for (StatData statData : data) {
            timeline.add(statData);
            try {
                Result result = indicator.calculate(timeline);
                indicators.add(result);
            } catch (Exception e) {
                e.printStackTrace();
                indicators.add(null);
            }

        }
        timeframeCache.put(timeframe, indicators);
        return indicators.get(historicalData.size() - 1);
    }
}
