package com.github.istin.tradingaizer.model;

import com.github.istin.tradingaizer.trader.StatDealData;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class KlineData implements StatDealData {
    private long openTime;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private double volume;
    private long closeTime;

    public String getOpenTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(openTime));
    }

    @Override
    public long getWhen() {
        return closeTime;
    }

    @Override
    public double getPrice() {
        return closePrice;
    }

    @Override
    public double getVolume() {
        return volume;
    }
}
