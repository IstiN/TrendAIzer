package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.FakeDealExecutor;
import com.github.istin.tradingaizer.report.ReportUtils;
import com.github.istin.tradingaizer.strategy.OptimizedStrategy;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.*;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StrategyTestingApp {

    static String TICKER = "BTCUSDT";

    public static void main(String[] args) {
        // 1) Load historical data once
        long endTime = System.currentTimeMillis();
        //long endTime = System.currentTimeMillis();
        long threeMonthsAgo = endTime - (1L * 7 * 24 * 60 * 60 * 1000);
        BinanceDataUtils.Result result = BinanceDataUtils.readDataFromBinance(TICKER, "1m", threeMonthsAgo, endTime);
        List<? extends StatDealData> fullHistory = result.historicalData();
        if (fullHistory.isEmpty()) {
            System.err.println("No data loaded!");
            return;
        }

        ChartDataProvider chartDataProvider = new ChartDataProvider(result.cacheId(), fullHistory);

        // 2) Strategy to test
        Strategy strategy = new OptimizedStrategy(result.cacheId(), chartDataProvider, 60, 40);

        // 3) Generate parameter ranges from 0.01 to 0.30 inclusive
        //    with step 0.01 => 0.01, 0.02, ..., 0.30

        // Params => maximumLoss=0.01, minimumProfit=0.04
        // Params => maximumLoss=0.01, minimumProfit=0.03

        double step = 0.01;
        double start = 0.1;
        double end   = 0.5;
        double[] maxLossRange = generateRange(0.01, 0.01, step);
        double[] minProfitRange = generateRange(0.04, 0.04d, step);

        // Tracking best result
        double bestFinalBalance = Double.NEGATIVE_INFINITY;
        double bestWinRate      = 0.0;
        double bestMaxLoss      = 0.0;
        double bestMinProfit    = 0.0;



        // 4) Loop over all param combos
        for (double minProfit : minProfitRange) {
            for (double maxLoss : maxLossRange) {

                // Create trader
                Trader trader = createTrader(maxLoss, minProfit);

                // Run simulation
                runSimulation(trader, strategy, fullHistory, TICKER);

                // Evaluate
                trader.calculateWinRate();
                double finalBalance = trader.getBalance();

                // Check if best
                if (finalBalance > bestFinalBalance) {
                    bestFinalBalance = finalBalance;
                    bestMaxLoss      = maxLoss;
                    bestMinProfit    = minProfit;
                }
            }
        }

        // 5) Print final best result
        System.out.println("=== OPTIMIZATION COMPLETE ===");
        System.out.println("Best finalBalance = " + bestFinalBalance);
        System.out.println("Best winRate      = " + bestWinRate);
        System.out.println("Params => maximumLoss=" + bestMaxLoss +
                ", minimumProfit=" + bestMinProfit);

        // (Optional) final run with best params, produce report
        Trader bestTrader = createTrader(bestMaxLoss, bestMinProfit);
        runSimulation(bestTrader, strategy, fullHistory, TICKER);
        List<Deal> closedDeals = bestTrader.getClosedDeals();
        ReportUtils.generateReport("trading_chart.html", closedDeals, fullHistory);
    }

    /**
     * Create a Trader with specific risk/reward parameters.
     */
    @NotNull
    private static Trader createTrader(double maximumLoss, double minimumProfit) {
        return new Trader(TICKER, maximumLoss, minimumProfit, 1d, new FakeDealExecutor(1000d));
    }

    /**
     * Run the entire simulation with a given strategy/trader/historical data.
     */
    private static void runSimulation(Trader trader, Strategy strategy, List<? extends StatDealData> history, String ticker) {
        List<StatData> timelineSimulation = new ArrayList<>();
        for (StatDealData data : history) {
            timelineSimulation.add(data);
            DecisionReason decisionReason = strategy.generateDecision(timelineSimulation);
            trader.decisionTrigger(ticker, decisionReason, data);
        }
    }

    /**
     * Generate a range of doubles from start to end (inclusive),
     * stepping by step. e.g. generateRange(0.01, 0.3, 0.01)
     */
    private static double[] generateRange(double start, double end, double step) {
        // Count how many steps
        int size = (int) Math.round(((end - start) / step)) + 1;
        double[] result = new double[size];
        double current = start;
        for (int i = 0; i < size; i++) {
            result[i] = Math.round(current * 100.0) / 100.0; // optional rounding
            current += step;
        }
        return result;
    }

}
