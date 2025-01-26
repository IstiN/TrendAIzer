package com.github.istin.tradingaizer.strategy;

import com.github.istin.tradingaizer.chart.ChartDataProvider;
import com.github.istin.tradingaizer.indicator.MACDIndicator;
import com.github.istin.tradingaizer.indicator.RSIIndicator;
import com.github.istin.tradingaizer.indicator.Timeframe;
import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public class BasicStrategy extends Strategy {

    public BasicStrategy(String cacheId,  ChartDataProvider chartDataProvider) {
        super(cacheId, chartDataProvider);
    }

    @Override
    public DecisionReason generateDecision(List<? extends StatData> historicalData) {
        Double rsi = calcOrFromCache(new RSIIndicator(14), historicalData, Timeframe.M1);
        MACDIndicator.Result macdResult = calcOrFromCache(new MACDIndicator(12, 26, 9), historicalData, Timeframe.M1);
        if (rsi == null || macdResult == null) {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
        System.out.println(rsi);
        System.out.println(macdResult.getMacd());
        System.out.println(macdResult.getSignalLine());
        if (rsi < 40 && macdResult.getMacd() > 0) {
            return new DecisionReason(Decision.LONG, "RSI is below 40 and MACD is bullish " + rsi + " " + macdResult.getMacd());
        } else if (rsi > 60 && macdResult.getMacd() < 0) {
            return new DecisionReason(Decision.SHORT, "RSI is above 60 and MACD is bearish " + rsi + " " + macdResult.getMacd());
        } else {
            return new DecisionReason(Decision.HOLD, "No clear signal");
        }
    }

}
