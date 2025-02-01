package com.github.istin.tradingaizer.trader;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Deal {
    private String ticker;
    private DealData openedData;
    private DealData closeData;
    private Direction direction;
    private double stopLoss;
    private double openAmountUSDT;
    private double closedAmount;
    private String message;

    public Deal(String ticker, DealData dealData, Direction direction, double tradeSizeUSDT) {
        this.ticker = ticker;
        this.openedData = dealData;
        this.direction = direction;
        this.openAmountUSDT = tradeSizeUSDT;
    }
}
