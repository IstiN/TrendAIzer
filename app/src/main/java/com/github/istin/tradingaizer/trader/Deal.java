package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.KlineData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Deal {
    private KlineData openedKlineData;
    private KlineData closedKlineData;
    private Direction direction;
    private double stopLoss;
    private double openAmount;
    private double closedAmount;
    private String message;

    Deal(double maximumLoss, KlineData openedKlineData, Direction direction, double tradeSize) {
        this.openedKlineData = openedKlineData;
        this.direction = direction;
        this.openAmount = tradeSize;
        this.stopLoss = direction == Direction.LONG ?
                openedKlineData.getOpenPrice() * (1 - maximumLoss) : openedKlineData.getOpenPrice() * (1 + maximumLoss);
    }
}
