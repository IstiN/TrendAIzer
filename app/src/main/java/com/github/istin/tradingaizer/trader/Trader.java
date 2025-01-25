package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Trader {

    @Getter
    private double balance;
    private double maximumLoss; // Stop-loss as a percentage
    private double minimumProfit; // Take-profit as a percentage
    private double riskPercentage; // Percentage of balance to risk per trade

    private final DealExecutor dealExecutor; // Interface instance

    @Getter
    private List<Deal> closedDeals = new ArrayList<>();

    private Deal currentDeal;

    public Trader(double balance, double maximumLoss, double minimumProfit, double riskPercentage, DealExecutor dealExecutor) {
        this.balance = balance;
        this.maximumLoss = maximumLoss;
        this.minimumProfit = minimumProfit;
        this.riskPercentage = riskPercentage;
        this.dealExecutor = dealExecutor;

        // Retrieve the current deal from the deal executor
        this.currentDeal = dealExecutor.getCurrentDeal();
        if (this.currentDeal != null) {
            System.out.printf("Existing deal loaded: %s at %.2f with trade size %.2f. Stop loss: %.2f%n",
                    currentDeal.getDirection(), currentDeal.getOpenedData().getPrice(),
                    currentDeal.getOpenAmount(), currentDeal.getStopLoss());
        }
    }

    public void decisionTrigger(String ticker, DecisionReason decisionReason, DealData dealData) {
        Decision decision = decisionReason.getDecision();
        if (currentDeal == null) {
            if (decision == Decision.LONG || decision == Decision.SHORT) {
                double tradeSize = balance * riskPercentage;
                currentDeal = new Deal(ticker, maximumLoss, dealData, decision == Decision.LONG ? Direction.LONG : Direction.SHORT, tradeSize);

                // Submit the deal using the interface
                dealExecutor.submitDeal(currentDeal);

                System.out.printf("[DEAL] New deal opened: %s at %.2f with trade size %.2f. Stop loss: %.2f Reason: %s%n",
                        currentDeal.getDirection(), dealData.getPrice(), tradeSize, currentDeal.getStopLoss(), decisionReason.getReason());
            }
        } else {
            double profitLoss = calculateProfitLoss(dealData.getPrice());
            double profitLossPercentage = profitLoss / balance;

            if (isStopLossTriggered(dealData.getPrice())) {
                closeDeal(dealData, "Stop loss triggered");
            } else if (profitLossPercentage >= minimumProfit) {
                closeDeal(dealData, "Minimum profit reached");
            } else if (decision == Decision.CLOSE) {
                closeDeal(dealData, "Close decision received: " + decisionReason.getReason());
            } else if ((currentDeal.getDirection() == Direction.LONG && decision == Decision.SHORT) ||
                    (currentDeal.getDirection() == Direction.SHORT && decision == Decision.LONG)) {
                closeDeal(dealData, "Opposite decision received: "  + decisionReason.getReason());
                decisionTrigger(ticker, decisionReason, dealData);
            } else {
                updateStopLoss(dealData.getPrice());
                System.out.printf("hold: %s %.2f, profit/loss: %.2f (%.2f%%), current price: %.2f, stop loss: %.2f%n",
                        currentDeal.getDirection(), currentDeal.getOpenedData().getPrice(),
                        profitLoss, profitLossPercentage * 100, dealData.getPrice(), currentDeal.getStopLoss());
            }
        }
    }

    private double calculateProfitLoss(double currentPrice) {
        if (currentDeal.getDirection() == Direction.LONG) {
            return currentDeal.getOpenAmount() * (currentPrice - currentDeal.getOpenedData().getPrice()) / currentDeal.getOpenedData().getPrice();
        } else {
            return currentDeal.getOpenAmount() * (currentDeal.getOpenedData().getPrice() - currentPrice) / currentDeal.getOpenedData().getPrice();
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

            // Update the stop loss through the deal executor
            dealExecutor.updateStopLoss(currentDeal, currentDeal.getStopLoss());

            System.out.printf("Stop loss updated to %.2f%n", currentDeal.getStopLoss());
        }
    }

    private void closeDeal(DealData dealData, String reason) {
        double profitLoss = calculateProfitLoss(dealData.getPrice());
        currentDeal.setClosedAmount(currentDeal.getOpenAmount() + profitLoss);
        currentDeal.setCloseData(dealData);

        // Use the deal executor to handle the closing logic
        dealExecutor.closeDeal(currentDeal, dealData.getPrice());

        balance += profitLoss;
        long openDuration = (dealData.getWhen() - currentDeal.getOpenedData().getWhen()) / 1000L / 60L;  // Duration in minutes
        String durationMessage = String.format("Open duration: %d min", openDuration);

        currentDeal.setMessage(String.format("[DEAL] Deal closed. Reason: %s. PL: %.2f. New balance: %.2f. %s",
                reason, profitLoss, balance, durationMessage));
        System.out.println(currentDeal.getMessage());

        closedDeals.add(currentDeal);
        currentDeal = null;
    }

    public double calculateWinRate() {
        if (closedDeals.isEmpty()) {
            System.out.println("No closed deals available to calculate win rate.");
            return 0.0;
        }

        long wins = closedDeals.stream().filter(deal -> deal.getClosedAmount() > deal.getOpenAmount()).count();
        long losses = closedDeals.size() - wins;

        double winPercentage = (double) wins / closedDeals.size() * 100;
        System.out.printf("[DEAL] Positive P/L deals: %d, Negative P/L deals: %d, winPercentage " + winPercentage, wins, losses);
        return winPercentage;
    }
}
