package com.github.istin.tradingaizer.trader;

// Define the DealExecutor interface
public interface DealExecutor {
    double getBalance();
    void submitDeal(Deal deal);
    void closeDeal(Deal deal, double closePrice);
    Deal getCurrentDeal(String ticker);
    void updateStopLoss(Deal deal, double newStopLoss);
}