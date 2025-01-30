package com.github.istin.tradingaizer.chart;

import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.indicator.Indicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.indicator.TimeframeAggregator;
import com.github.istin.tradingaizer.trader.StatData;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChartDataProvider {

    private static final String CACHE_FOLDER = "indicatorsCache";
    private final String cacheId;
    private final List<? extends StatData> data1m;
    private final List<StatData> data1h;
    private final List<StatData> data15m;
    private final List<StatData> data5m;
    private final Map<String, Map<String, List<?>>> indicatorCache = new ConcurrentHashMap<>();
    private Boolean isIndicatorCacheEnabled = false;

    public ChartDataProvider(String cacheId, List<? extends StatData> statData1MinutTimeframe) {
        this.cacheId = cacheId;
        this.data1m = statData1MinutTimeframe;
        this.data5m = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.M5);
        this.data15m = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.M15);
        this.data1h = TimeframeAggregator.convertToTimeframe(this.data1m, Timeframe.H1);
        isIndicatorCacheEnabled = new ConfigReader().getConfig().getIndicatorCache();
        restoreCache();
    }

    private void restoreCache() {
        if (isIndicatorCacheEnabled) {
            File cacheFile = new File(CACHE_FOLDER, cacheId + ".ser");
            if (cacheFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
                    Map<String, Map<String, List<?>>> restoredCache = (Map<String, Map<String, List<?>>>) ois.readObject();
                    indicatorCache.putAll(restoredCache);
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Failed to restore cache: " + e.getMessage());
                }
            }
        }
    }

    private void saveCache() {
        if (isIndicatorCacheEnabled) {
            File cacheDir = new File(CACHE_FOLDER);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File cacheFile = new File(cacheDir, cacheId + ".ser");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                oos.writeObject(indicatorCache);
            } catch (IOException e) {
                System.err.println("Failed to save cache: " + e.getMessage());
            }
        }
    }

    public List<? extends StatData> getData(List<? extends StatData> historicalData, Timeframe timeframe) {
        int size = historicalData.size();
        switch (timeframe) {
            case M1:
                return data1m.subList(0, size - 1);
            case M5:
                if (size < 5) {
                    return Collections.emptyList();
                }
                return data5m.subList(0, size /5 - 1);
            case M15:
                if (size < 15) {
                    return Collections.emptyList();
                }
                return data15m.subList(0, size /15 - 1);
            case H1:
                if (size < 60) {
                    return Collections.emptyList();
                }
                return data1h.subList(0, size /60 - 1);
            default:
                throw new IllegalStateException("Unexpected value: " + timeframe);
        }
    }

    public <Result> Result calculateIndicator(Indicator<Result> indicator, List<? extends StatData> historicalData, Timeframe timeframe) {
        String indicatorKey = indicator.toString();

        // Ensure thread-safe access to cache for the given indicator
        indicatorCache.putIfAbsent(indicatorKey, new ConcurrentHashMap<>());
        Map<String, List<?>> timeframeCache = indicatorCache.get(indicatorKey);

        // Check if the values are already cached for this timeframe
        String name = timeframe.name();
        if (timeframeCache.containsKey(name)) {
            List<?> cachedResults = timeframeCache.get(name);
            if (cachedResults.size() >= historicalData.size()) {
                // Use the cached result directly if available
                return (Result) cachedResults.get(historicalData.size() - 1);
            }
        }

        // Perform calculation if no cache is available
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
        int i = 0;
        for (StatData statData : data) {
            timeline.add(statData);

            int lastSize = 1000;
            List<StatData> lastItems = new ArrayList<>();
            if (timeline.size() < lastSize) {
                lastItems = timeline;
            } else {
                // 2) Sub-list for last 100 (or 1000, your choice)
                lastItems = timeline.subList(
                        Math.max(0, timeline.size() - lastSize),
                        timeline.size()
                );
            }



            try {
                Result result = indicator.calculate(lastItems);
                indicators.add(result);
                System.out.println("Indicator: " + indicatorKey + ", Timeframe: " + name + ", Index: " + i + ", From: "+ data.size() +",  Value: " + result);
            } catch (Exception e) {
                e.printStackTrace();
                indicators.add(null);
            }
            i++;
        }

        // Update cache
        timeframeCache.put(name, indicators);
        saveCache();

        // Return the last calculated result
        return indicators.get(historicalData.size() - 1);
    }
}
