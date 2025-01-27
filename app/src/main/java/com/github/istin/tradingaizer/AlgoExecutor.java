package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.config.Config;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.report.ReportUtils;
import com.github.istin.tradingaizer.strategy.OptimizedStrategy;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.*;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.ArrayList;
import java.util.List;

public class AlgoExecutor {

    public static void main(String[] args) {
        Config config = new ConfigReader().getConfig();
        BinanceDealExecutor dealExecutor = new BinanceDealExecutor(config.getApiKey(), config.getApiSecret());
        //SIZE is in target crypt
        Deal deal = new Deal("XRPUSDT", 0.01, new KlineData(), Direction.LONG, 2);
        dealExecutor.submitDeal(deal);
        //Deal currentDeal = dealExecutor.getCurrentDeal();
        //System.out.println(currentDeal);
        //dealExecutor.submitDeal(deal, 0);
//        long now = System.currentTimeMillis();
//        long monthAgo = now - ((long) 30 * 24 * 60 * 60 * 1000);
//        BinanceDataUtils.Result result = BinanceDataUtils.readDataFromBinance("BTCUSDT", "5m", monthAgo, now);
//        OptimizedStrategy optimizedStrategy = new OptimizedStrategy(result.cacheId());
//        Decision currentDecision = optimizedStrategy.generateDecision(result.historicalData());
//
//        Trader trader = new Trader(1000d, 0.03d, 0.04d, 0.5d, dealExecutor);
//        System.out.println(currentDecision);

    }

}
