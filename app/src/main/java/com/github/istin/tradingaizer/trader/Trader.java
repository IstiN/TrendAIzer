package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Trader {

    @Getter
    private double balance;
    private double maximumLoss; // Stop-loss as a percentage
    private double minimumProfit; // Take-profit as a percentage
    private double riskPercentage; // Percentage of balance to risk per trade

    public Trader(double balance, double maximumLoss, double minimumProfit, double riskPercentage) {
        this.balance = balance;
        this.maximumLoss = maximumLoss;
        this.minimumProfit = minimumProfit;
        this.riskPercentage = riskPercentage;
    }

    @Getter
    private List<Deal> closedDeals = new ArrayList<>();

    private Deal currentDeal;

    public void decisionTrigger(Decision decision, KlineData klineData) {
        if (currentDeal == null) {
            if (decision == Decision.LONG || decision == Decision.SHORT) {
                double tradeSize = balance * riskPercentage;
                currentDeal = new Deal(maximumLoss, klineData, decision == Decision.LONG ? Direction.LONG : Direction.SHORT, tradeSize);
                System.out.printf("New deal opened: %s at %.2f with trade size %.2f. Stop loss: %.2f%n",
                        currentDeal.getDirection(), klineData.getOpenPrice(), tradeSize, currentDeal.getStopLoss());
            }
        } else {
            double profitLoss = calculateProfitLoss(klineData.getClosePrice());
            double profitLossPercentage = profitLoss / balance;

            if (isStopLossTriggered(klineData.getClosePrice())) {
                closeDeal(klineData, "Stop loss triggered");
            } else if (profitLossPercentage >= minimumProfit) {
                closeDeal(klineData, "Minimum profit reached");
            } else if (decision == Decision.CLOSE) {
                closeDeal(klineData, "Close decision received");
            } else if ((currentDeal.getDirection() == Direction.LONG && decision == Decision.SHORT) ||
                    (currentDeal.getDirection() == Direction.SHORT && decision == Decision.LONG)) {
                closeDeal(klineData, "Opposite decision received");
                decisionTrigger(decision, klineData);
            } else {
                updateStopLoss(klineData.getClosePrice());
                System.out.printf("Deal hold: %s %.2f, profit/loss: %.2f (%.2f%%), current price: %.2f, stop loss: %.2f%n",
                        currentDeal.getDirection(), currentDeal.getOpenedKlineData().getOpenPrice(),
                        profitLoss, profitLossPercentage * 100, klineData.getClosePrice(), currentDeal.getStopLoss());
            }
        }
    }

    private double calculateProfitLoss(double currentPrice) {
        if (currentDeal.getDirection() == Direction.LONG) {
            return currentDeal.getOpenAmount() * (currentPrice - currentDeal.getOpenedKlineData().getOpenPrice()) / currentDeal.getOpenedKlineData().getOpenPrice();
        } else {
            return currentDeal.getOpenAmount() * (currentDeal.getOpenedKlineData().getOpenPrice() - currentPrice) / currentDeal.getOpenedKlineData().getOpenPrice();
        }
    }

    private boolean isStopLossTriggered(double currentPrice) {
        return (currentDeal.getDirection() == Direction.LONG && currentPrice <= currentDeal.getStopLoss()) ||
                (currentDeal.getDirection() == Direction.SHORT && currentPrice >= currentDeal.getStopLoss());
    }

    private void updateStopLoss(double currentPrice) {
        double profitPercentage = calculateProfitLoss(currentPrice) / balance;
        if (profitPercentage > 0) {
            double newStopLoss;
            if (currentDeal.getDirection() == Direction.LONG) {
                newStopLoss = currentPrice * (1 - maximumLoss / 2); // Tighten stop-loss in profit
                currentDeal.setStopLoss(Math.max(currentDeal.getStopLoss(), newStopLoss));
            } else {
                newStopLoss = currentPrice * (1 + maximumLoss / 2); // Tighten stop-loss in profit
                currentDeal.setStopLoss(Math.min(currentDeal.getStopLoss(), newStopLoss));
            }
            System.out.printf("Stop loss updated to %.2f%n", currentDeal.getStopLoss());
        }
    }

    // Update the closeDeal method
    private void closeDeal(KlineData klineData, String reason) {
        double profitLoss = calculateProfitLoss(klineData.getClosePrice());
        currentDeal.setClosedAmount(currentDeal.getOpenAmount() + profitLoss);
        currentDeal.setClosedKlineData(klineData);

        balance += profitLoss;
        long openDuration = (klineData.getCloseTime() - currentDeal.getOpenedKlineData().getCloseTime()) / 1000l / 60l;  // Duration in milliseconds
        String durationMessage = String.format("Open duration: %d min", openDuration);

        currentDeal.setMessage(String.format("Deal closed. Reason: %s. PL: %.2f. New balance: %.2f. %s",
                reason, profitLoss, balance, durationMessage));
        System.out.println(currentDeal.getMessage());

        closedDeals.add(currentDeal);
        currentDeal = null;
    }
}
