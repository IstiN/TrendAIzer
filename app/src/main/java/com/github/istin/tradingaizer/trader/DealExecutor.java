package com.github.istin.tradingaizer.trader;

// Define the DealExecutor interface
public interface DealExecutor {
    void submitDeal(Deal deal);
    void closeDeal(Deal deal, double closePrice);
    Deal getCurrentDeal(); // Method to retrieve the current deal
    void updateStopLoss(Deal deal, double newStopLoss);
}