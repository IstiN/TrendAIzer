package com.github.istin.tradingaizer.utils;

import com.github.istin.tradingaizer.config.Config;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.provider.BinanceDataProvider;
import com.github.istin.tradingaizer.provider.BinanceFutureDataProvider;
import com.github.istin.tradingaizer.provider.DataProvider;
import com.github.istin.tradingaizer.trader.StatDealData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BinanceDataUtils {
    public static long MONTH = 1L * 30 * 24 * 60 * 60 * 1000;

    public static Result readBtcHistoricalData() {
//        return readBtcHistoricalData(0);
//        return readBtcHistoricalData(22);
        return readBtcHistoricalData(23);
//        return readBtcHistoricalData(24);
    }

    @NotNull
    public static Result readBtcHistoricalData(int months) {
        long endTime = 1737806638768L;

        endTime = endTime - months*MONTH;
        System.out.println(endTime);
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - MONTH;

        //String ticker = "BTCUSDT";
        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readXrpHistoricalData() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (1L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "XRPUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readETHHistoricalData() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (1L * 30 * 24 * 60 * 60 * 1000);

        String ticker = "ETHUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readETHHistoricalDataFromJuly24() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (7L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readBtcHistoricalDataFromJuly24() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (7L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readBtcHistoricalDataLastYear() {
        long endTime = 1737806638768L;
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (12L * 30 * 24 * 60 * 60 * 1000);

        //String ticker = "BTCUSDT";
        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, threeMonthsAgo, endTime);
    }

    @NotNull
    public static Result readBtcHistoricalDataNov2021ToNov2022() {
        // For example, from November 1, 2021 at 00:00:00 UTC to November 1, 2022 at 00:00:00 UTC:
        long startTime = 1635724800000L; // 2021-11-01 00:00:00 UTC
        long endTime   = 1667260800000L; // 2022-11-01 00:00:00 UTC

        String ticker = "BTCUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, startTime, endTime);
    }

    @NotNull
    public static Result readXrpHistoricalDataNov2021ToNov2022() {
        // For example, from November 1, 2021 at 00:00:00 UTC to November 1, 2022 at 00:00:00 UTC:
        long startTime = 1635724800000L; // 2021-11-01 00:00:00 UTC
        long endTime   = 1667260800000L; // 2022-11-01 00:00:00 UTC

        String ticker = "XRPUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, startTime, endTime);
    }

    @NotNull
    public static Result readEthHistoricalDataNov2021ToNov2022() {
        // For example, from November 1, 2021 at 00:00:00 UTC to November 1, 2022 at 00:00:00 UTC:
        long startTime = 1635724800000L; // 2021-11-01 00:00:00 UTC
        long endTime   = 1667260800000L; // 2022-11-01 00:00:00 UTC

        String ticker = "ETHUSDT";
        String interval = "1m";

        return readDataFromBinance(ticker, interval, startTime, endTime);
    }


    @NotNull
    public static Result readDataFromBinance(String ticker, String interval, long startTime, long endTime) {
        ConfigReader configReader = new ConfigReader();
        Config config = configReader.getConfig();
        DataProvider dataProvider = new BinanceDataProvider(config.getApiKey(), config.getApiSecret());
        //long endTime = System.currentTimeMillis();

        String cacheId = ticker + "_" + interval + "_" + endTime + "_" + startTime;

        List<? extends StatDealData> historicalData = dataProvider.fetchHistoricalData(ticker, interval, startTime, endTime);
        return new Result(cacheId, historicalData);
    }

    @NotNull
    public static Result readDataFromBinanceFuture(String ticker, String interval, long startTime, long endTime) {
        ConfigReader configReader = new ConfigReader();
        Config config = configReader.getConfig();
        DataProvider dataProvider = new BinanceFutureDataProvider(config.getApiKey(), config.getApiSecret());
        //long endTime = System.currentTimeMillis();

        String cacheId = ticker + "_" + interval + "_" + endTime + "_" + startTime;

        List<? extends StatDealData> historicalData = dataProvider.fetchHistoricalData(ticker, interval, startTime, endTime);
        return new Result(cacheId, historicalData);
    }

    public record Result(String cacheId, List<? extends StatDealData> historicalData) {
    }
}
