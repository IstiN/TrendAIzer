package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.List;

public abstract class Indicator<Result> {

    public abstract Result calculate(List<StatData> historicalData);

}
