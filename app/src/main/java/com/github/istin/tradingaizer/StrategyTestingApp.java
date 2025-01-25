package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.report.ReportUtils;
import com.github.istin.tradingaizer.strategy.*;
import com.github.istin.tradingaizer.trader.Deal;
import com.github.istin.tradingaizer.trader.DealExecutor;
import com.github.istin.tradingaizer.trader.StatData;
import com.github.istin.tradingaizer.trader.Trader;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.ArrayList;
import java.util.List;

public class StrategyTestingApp {

    public static void main(String[] args) {
        BinanceDataUtils.Result result = BinanceDataUtils.readBtcHistoricalData();

        List<StatData> timelineSimulation = new ArrayList<>();

        List<Strategy> strategies = new ArrayList<>();
        ChartDataProvider chartDataProvider = new ChartDataProvider(result.historicalData());
        strategies.add(new BasicStrategy(result.cacheId(), chartDataProvider));
        //strategies.add(new AdvancedStrategy(result.cacheId()));
        //strategies.add(new ChartBasedStrategy(result.cacheId()));
        //strategies.add(new OptimizedStrategy(result.cacheId()));

        //strategies.add(new EnhancedStrategy1(result.cacheId(), chartDataProvider));
        String ticker = "BTCUSDT";
        Trader trader = new Trader(1000d, 0.3d, 0.04d, 1d, new DealExecutor() {
            @Override
            public void submitDeal(Deal deal) {
                //fake deal submition
            }

            @Override
            public void closeDeal(Deal deal, double closePrice) {
                //fake deal closing
            }

            @Override
            public Deal getCurrentDeal() {
                //no opened deals at beginning
                return null;
            }

            @Override
            public void updateStopLoss(Deal deal, double newStopLoss) {

            }
        });

        for (Strategy strategy : strategies) {
            for (KlineData data : result.historicalData()) {
                timelineSimulation.add(data);
                try {
                    DecisionReason decisionReason = strategy.generateDecision(timelineSimulation);
                    trader.decisionTrigger(ticker, decisionReason, data);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    trader.decisionTrigger(ticker, new DecisionReason(Decision.HOLD, "no clear signal found"), data);
                }
            }

        }

        List<Deal> closedDeals = trader.getClosedDeals();
        ReportUtils.generateReport("trading_chart.html", closedDeals, result.historicalData());
        trader.calculateWinRate();
    }

}
