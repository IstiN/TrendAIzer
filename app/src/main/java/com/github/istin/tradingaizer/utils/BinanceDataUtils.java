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
        ConfigReader configReader = new ConfigReader();
        configReader.loadConfig();
        Config config = configReader.getConfig();
        DataProvider dataProvider = new BinanceDataProvider(config.getApiKey(), config.getApiSecret());
        //long endTime = System.currentTimeMillis();
        long endTime = 1737621533553L;
        long threeMonthsAgo = endTime - (1L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "XRPUSDT";
        String interval = "5m";
        String cacheId = ticker + "_" + interval + "_" + endTime + "_" + threeMonthsAgo;

        List<KlineData> historicalData = dataProvider.fetchHistoricalData(ticker, interval, threeMonthsAgo, endTime);
        return new Result(cacheId, historicalData);
    }

    public record Result(String cacheId, List<KlineData> historicalData) {
    }
}
