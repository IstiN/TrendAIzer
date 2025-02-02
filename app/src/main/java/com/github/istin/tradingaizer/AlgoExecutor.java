package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.config.Config;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.strategy.OptimizedStrategy;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.BinanceDealExecutor;
import com.github.istin.tradingaizer.trader.StatDealData;
import com.github.istin.tradingaizer.trader.Trader;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.List;

public class AlgoExecutor {

    public static void main(String[] args) {
        Config config = new ConfigReader().getConfig();
        String ticker = "BTCUSDT";
        long now = System.currentTimeMillis();
        long startTime = now - 20 * 60 * 60 * 1000;
        BinanceDataUtils.Result result = BinanceDataUtils.readDataFromBinanceFuture(ticker, "5m", startTime, now);

        BinanceDealExecutor dealExecutor = new BinanceDealExecutor(config.getApiKey(), config.getApiSecret());
        List<? extends StatDealData> historicalData = result.historicalData();
        ChartDataProvider chartDataProvider = new ChartDataProvider(result.cacheId(), historicalData);
        Strategy strategy = new OptimizedStrategy(result.cacheId(), chartDataProvider, 60, 40);
        Trader trader = new Trader(ticker, 0.01d, 0.04, 0.9d, dealExecutor);
        DecisionReason decisionReason = strategy.generateDecision(historicalData);
        StatDealData statDealData = historicalData.getLast();
        System.out.println(decisionReason);
        //decisionReason = new DecisionReason(Decision.LONG, "Test");
        System.out.println("Current balance: " + trader.getBalance());
        trader.decisionTrigger(ticker, decisionReason, statDealData);
    }
}
