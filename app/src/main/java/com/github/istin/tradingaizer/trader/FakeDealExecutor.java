package com.github.istin.tradingaizer.trader;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FakeDealExecutor implements DealExecutor {

    private double balance;
    private double stopLoss;
    private Deal currentDeal;

    public FakeDealExecutor(double balance) {
        this.balance = balance;
    }

    @Override
    public void submitDeal(Deal deal) {
        currentDeal = deal;
    }

    @Override
    public void closeDeal(Deal deal, double closePrice) {
        currentDeal = null;
    }

    @Override
    public Deal getCurrentDeal(String ticker) {
        return currentDeal;
    }

    @Override
    public void updateStopLoss(Deal deal, double newStopLoss) {
        stopLoss = newStopLoss;
    }
}
