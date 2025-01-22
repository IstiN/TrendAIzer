package com.github.istin.tradingaizer.provider;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public interface DataProvider {
    List<KlineData> fetchHistoricalData(String symbol, String interval, long startTime, long endTime);
}
