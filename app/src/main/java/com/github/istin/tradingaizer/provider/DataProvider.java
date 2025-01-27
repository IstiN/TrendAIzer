package com.github.istin.tradingaizer.provider;

import com.github.istin.tradingaizer.trader.StatDealData;

import java.util.List;

public interface DataProvider {
    List<? extends StatDealData> fetchHistoricalData(String symbol, String interval, long startTime, long endTime);
}
