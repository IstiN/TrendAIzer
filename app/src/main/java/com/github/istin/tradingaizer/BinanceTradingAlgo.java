package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.report.ReportUtils;
import com.github.istin.tradingaizer.strategy.*;
import com.github.istin.tradingaizer.trader.Deal;
import com.github.istin.tradingaizer.trader.Trader;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.*;

public class BinanceTradingAlgo {

    public static void main(String[] args) {
        BinanceDataUtils.Result result = BinanceDataUtils.readBtcHistoricalData();

        List<KlineData> timelineSimulation = new ArrayList<>();

        List<Strategy> strategies = new ArrayList<>();

        //strategies.add(new BasicStrategy(result.cacheId()));
        //strategies.add(new AdvancedStrategy(result.cacheId()));
        //strategies.add(new ChartBasedStrategy(result.cacheId()));
        strategies.add(new OptimizedStrategy(result.cacheId()));
        Trader trader = new Trader(1000d, 0.03d, 0.04d, 0.5d);
        for (Strategy strategy : strategies) {

            for (KlineData data : result.historicalData()) {
                timelineSimulation.add(data);
                try {
                    Decision decision = strategy.generateDecision(timelineSimulation);
                    trader.decisionTrigger(decision, data);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

        }

        List<Deal> closedDeals = trader.getClosedDeals();
        ReportUtils.generateReport("trading_chart.html", closedDeals, result.historicalData());
    }

}
