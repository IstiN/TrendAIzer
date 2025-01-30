package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.config.Config;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.strategy.OptimizedStrategy;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.*;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.List;

public class AlgoExecutor {

    public static void main(String[] args) {
        Config config = new ConfigReader().getConfig();
        String ticker = "BTCUSDT";
        long now = System.currentTimeMillis();
        long startTime = now - 20 * 60 * 60 * 1000;
        BinanceDataUtils.Result result = BinanceDataUtils.readDataFromBinance(ticker, "1m", startTime, now);
        BinanceDealExecutor dealExecutor = new BinanceDealExecutor(config.getApiKey(), config.getApiSecret());
        List<? extends StatDealData> historicalData = result.historicalData();
        ChartDataProvider chartDataProvider = new ChartDataProvider(result.cacheId(), historicalData);
        Strategy strategy = new OptimizedStrategy(result.cacheId(), chartDataProvider, 60, 40);
        Trader trader = new Trader(ticker, 0.01d, 0.04, 0.9d, dealExecutor);
        DecisionReason decisionReason = strategy.generateDecision(historicalData);
        StatDealData statDealData = historicalData.getLast();
        trader.decisionTrigger(ticker, decisionReason, statDealData);


//        double priceUsdt = 6;
        //SIZE is in target crypt
//        TODO Deal deal = new Deal(ticker, 0.01, new KlineData(), Direction.LONG,  Math.round(priceUsdt / statDealData.getPrice() * 10.0) / 10.0);
//        Deal deal = new Deal(ticker, 0.01, historicalData.getLast(), Direction.LONG,  priceUsdt);
//        dealExecutor.submitDeal(deal);
//        Deal currentDeal = dealExecutor.getCurrentDeal(ticker);
//        System.out.println(currentDeal);
//        double balance = dealExecutor.getBalance();
//        System.out.println(balance);
        //currentDeal.setStopLoss(2.9);
        //dealExecutor.updateStopLoss(currentDeal, 105000);
//        dealExecutor.closeDeal(currentDeal, 0);
        //dealExecutor.submitDeal(deal);

//        balance = dealExecutor.getBalance();
//        System.out.println(balance);
//        long now = System.currentTimeMillis();
//        long monthAgo = now - ((long) 30 * 24 * 60 * 60 * 1000);
//        BinanceDataUtils.Result result = BinanceDataUtils.readDataFromBinance("BTCUSDT", "5m", monthAgo, now);
//        OptimizedStrategy optimizedStrategy = new OptimizedStrategy(result.cacheId());
//        Decision currentDecision = optimizedStrategy.generateDecision(result.historicalData());
//
//        Trader trader = new Trader(ticker, 0.03d, 0.04d, 0.5d, dealExecutor);
//        System.out.println(currentDecision);

    }

}
