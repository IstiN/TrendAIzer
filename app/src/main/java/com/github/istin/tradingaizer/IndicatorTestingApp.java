package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.report.ReportUtils;
import com.github.istin.tradingaizer.strategy.BasicStrategy;
import com.github.istin.tradingaizer.strategy.EnhancedStrategyV2;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.Deal;
import com.github.istin.tradingaizer.trader.DealExecutor;
import com.github.istin.tradingaizer.trader.StatData;
import com.github.istin.tradingaizer.trader.Trader;
import com.github.istin.tradingaizer.utils.BinanceDataUtils;

import java.util.ArrayList;
import java.util.List;

public class IndicatorTestingApp {

    public static void main(String[] args) {
        BinanceDataUtils.Result result = BinanceDataUtils.readBtcHistoricalData();
        ChartDataProvider chartDataProvider = new ChartDataProvider(result.historicalData());
        EnhancedStrategyV2 basicStrategy = new EnhancedStrategyV2(result.cacheId(), chartDataProvider);
        System.out.println(basicStrategy.generateDecision(result.historicalData()));
        System.out.println(basicStrategy.generateDecision(result.historicalData()));
    }

}
