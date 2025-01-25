package com.github.istin.tradingaizer.utils;

import com.github.istin.tradingaizer.config.Config;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.provider.BinanceDataProvider;
import com.github.istin.tradingaizer.provider.DataProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BinanceDataUtils {
    @NotNull
    public static Result readBtcHistoricalData() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (7L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readDataFromBinance(String ticker, String interval, long startTime, long endTime) {
        ConfigReader configReader = new ConfigReader();
        Config config = configReader.getConfig();
        DataProvider dataProvider = new BinanceDataProvider(config.getApiKey(), config.getApiSecret());
        //long endTime = System.currentTimeMillis();

        String cacheId = ticker + "_" + interval + "_" + endTime + "_" + startTime;

        List<KlineData> historicalData = dataProvider.fetchHistoricalData(ticker, interval, startTime, endTime);
        return new Result(cacheId, historicalData);
    }

    public record Result(String cacheId, List<KlineData> historicalData) {
    }
}
