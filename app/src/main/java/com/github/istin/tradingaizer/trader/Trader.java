package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Trader {

    @Getter
    private double balance;
    private double maximumLoss;
    private double minimumProfit;
    private double riskPercentage;

    public Trader(double balance, double maximumLoss, double minimumProfit, double riskPercentage) {
        this.balance = balance;
        this.maximumLoss = maximumLoss;
        this.minimumProfit = minimumProfit;
        this.riskPercentage = riskPercentage;
    }

    @Getter
    private List<Deal> closedDeals = new ArrayList<>();

    private enum Direction {
        LONG, SHORT
    }

    @Data
    public class Deal {
        private KlineData openedKlineData;
        private KlineData closedKlineData;
        private Direction direction;
        private double stopLoss;
        private double openAmount;
        private double closedAmount;
        private String message;

        Deal(KlineData openedKlineData, Direction direction, double tradeSize) {
            this.openedKlineData = openedKlineData;
            this.direction = direction;
            this.openAmount = tradeSize;
            this.stopLoss = direction == Direction.LONG ?
                    openedKlineData.getOpenPrice() * (1 - maximumLoss) : openedKlineData.getOpenPrice() * (1 + maximumLoss);
        }
    }

    private Deal currentDeal;

    public void decisionTrigger(Decision decision, KlineData klineData) {
        if (currentDeal == null) {
            if (decision == Decision.LONG || decision == Decision.SHORT) {
                double tradeSize = balance * riskPercentage;
                currentDeal = new Deal(klineData, decision == Decision.LONG ? Direction.LONG : Direction.SHORT, tradeSize);
                System.out.printf("New deal opened: %s at %.2f with trade size %.2f. Stop loss: %.2f%n",
                        currentDeal.direction, klineData, tradeSize, currentDeal.stopLoss);
            }
        } else {
            double profitLoss = calculateProfitLoss(klineData.getOpenPrice());
            double profitLossPercentage = profitLoss / balance;

            if (isStopLossTriggered(klineData.getOpenPrice())) {
                closeDeal(klineData, "Stop loss triggered");
            } else if (profitLossPercentage >= minimumProfit) {
                closeDeal(klineData, "Minimum profit reached");
            } else if ((currentDeal.direction == Direction.LONG && decision == Decision.SHORT) ||
                    (currentDeal.direction == Direction.SHORT && decision == Decision.LONG)) {
                closeDeal(klineData, "Opposite decision received");
                decisionTrigger(decision, klineData);
            } else {
                updateStopLoss(klineData.getOpenPrice());
                System.out.printf("Deal hold: %s %.2f, profit/loss: %.2f (%.2f%%), current price: %.2f, stop loss: %.2f%n",
                        currentDeal.direction, currentDeal.openedKlineData, profitLoss, profitLossPercentage * 100, klineData, currentDeal.stopLoss);
            }
        }
    }

    private double calculateProfitLoss(double currentPrice) {
        if (currentDeal.direction == Direction.LONG) {
            return currentDeal.openAmount * (currentPrice - currentDeal.openedKlineData.getOpenPrice()) / currentDeal.openedKlineData.getOpenPrice();
        } else {
            return currentDeal.openAmount * (currentDeal.openedKlineData.getOpenPrice() - currentPrice) / currentDeal.openedKlineData.getOpenPrice();
        }
    }

    private boolean isStopLossTriggered(double currentPrice) {
        return (currentDeal.direction == Direction.LONG && currentPrice <= currentDeal.stopLoss) ||
                (currentDeal.direction == Direction.SHORT && currentPrice >= currentDeal.stopLoss);
    }

    private void updateStopLoss(double currentPrice) {
        double profitPercentage = (calculateProfitLoss(currentPrice) / balance);
        if (profitPercentage > 0) {
            double newStopLoss;
            if (currentDeal.direction == Direction.LONG) {
                newStopLoss = currentPrice * (1 - maximumLoss / 2); // Tighter stop-loss in profit
                currentDeal.stopLoss = Math.max(currentDeal.stopLoss, newStopLoss);
            } else {
                newStopLoss = currentPrice * (1 + maximumLoss / 2); // Tighter stop-loss in profit
                currentDeal.stopLoss = Math.min(currentDeal.stopLoss, newStopLoss);
            }
            System.out.printf("Stop loss updated: %.2f%n", currentDeal.stopLoss);
        }
    }

    private void closeDeal(KlineData klineData, String reason) {
        double profitLoss = calculateProfitLoss(klineData.getClosePrice());
        currentDeal.closedAmount = currentDeal.openAmount + profitLoss;
        currentDeal.closedKlineData = klineData;
        balance += profitLoss;
        currentDeal.message = String.format("Deal closed. Reason: %s. PL: %.2f. New balance: %.2f", reason, profitLoss, balance);
        System.out.println(currentDeal.message);
        closedDeals.add(currentDeal);
        currentDeal = null;
    }
}
