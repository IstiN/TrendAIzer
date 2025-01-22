package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;

import java.util.List;

public abstract class Indicator {

    public abstract double calculate(List<KlineData> historicalData);

}
