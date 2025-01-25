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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StrategyTestingApp {

    public static void main(String[] args) {
        BinanceDataUtils.Result result = BinanceDataUtils.readBtcHistoricalData();


        List<Strategy> strategies = new ArrayList<>();
        ChartDataProvider chartDataProvider = new ChartDataProvider(result.cacheId(), result.historicalData());
//        strategies.add(new BasicStrategy(result.cacheId(), chartDataProvider));
        strategies.add(new AdaptiveMultiTimeframeStrategy(result.cacheId(), chartDataProvider));
        //strategies.add(new AdvancedStrategy(result.cacheId(), chartDataProvider));
//        strategies.add(new ChartBasedStrategy(result.cacheId(), chartDataProvider));
//        strategies.add(new EnhancedStrategy(result.cacheId(), chartDataProvider));
//        strategies.add(new EnhancedStrategy1(result.cacheId(), chartDataProvider));
//        strategies.add(new EnhancedStrategy2(result.cacheId(), chartDataProvider));
        //strategies.add(new OptimizedStrategy(result.cacheId(), chartDataProvider));

        //strategies.add(new EnhancedStrategyV2(result.cacheId(), chartDataProvider));

        String ticker = "BTCUSDT";
        for (Strategy strategy : strategies) {
            List<StatData> timelineSimulation = new ArrayList<>();
            System.out.println(strategy.getClass().getSimpleName() + " starting");
            Trader trader = createTrader();
            for (KlineData data : result.historicalData()) {
                timelineSimulation.add(data);
                DecisionReason decisionReason = strategy.generateDecision(timelineSimulation);
                trader.decisionTrigger(ticker, decisionReason, data);
            }
            List<Deal> closedDeals = trader.getClosedDeals();
            ReportUtils.generateReport("trading_chart.html", closedDeals, result.historicalData());
            trader.calculateWinRate();
            System.out.println(" " + strategy.getClass().getSimpleName() + " ended");
        }


    }

    @NotNull
    private static Trader createTrader() {
        Trader trader = new Trader(1000d, 0.03d, 0.04d, 1d, new DealExecutor() {
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
        return trader;
    }

}
