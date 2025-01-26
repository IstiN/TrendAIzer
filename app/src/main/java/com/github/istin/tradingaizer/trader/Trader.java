package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trader {

    private double sumLoss;
    private double sumProfit;
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
                // Use dynamic stop-loss if provided, otherwise fall back to default logic
                double tradeSize = balance * riskPercentage;
                double stopLoss = decisionReason.getDynamicStopLoss() != null
                        ? decisionReason.getDynamicStopLoss()
                        : calculateDefaultStopLoss(dealData.getPrice(), decision == Decision.LONG);

                currentDeal = new Deal(ticker, stopLoss, dealData,
                        decision == Decision.LONG ? Direction.LONG : Direction.SHORT, tradeSize);

                dealExecutor.submitDeal(currentDeal);

                System.out.printf("[DEAL] New deal opened: %s at %.2f with trade size %.2f. Stop loss: %.2f Reason: %s%n " + convertToDateTime(dealData),
                        currentDeal.getDirection(), dealData.getPrice(), tradeSize, currentDeal.getStopLoss(), decisionReason.getReason());
            }
        } else {
            double profitLoss = calculateProfitLoss(dealData.getPrice());
            double profitLossPercentage = profitLoss / balance;

            if (isStopLossTriggered(dealData.getPrice())) {
                closeDeal(dealData, "Stop loss triggered");
            } else if (profitLossPercentage >= minimumProfit || (decisionReason.getTakeProfit() != null && dealData.getPrice() >= decisionReason.getTakeProfit())) {
                closeDeal(dealData, "Take-profit reached");
            } else if (decision == Decision.CLOSE) {
                closeDeal(dealData, "Close decision received: " + decisionReason.getReason());
            } else if ((currentDeal.getDirection() == Direction.LONG && decision == Decision.SHORT) ||
                    (currentDeal.getDirection() == Direction.SHORT && decision == Decision.LONG)) {
                closeDeal(dealData, "Opposite decision received: " + decisionReason.getReason());
                decisionTrigger(ticker, decisionReason, dealData); // Trigger the new deal after closing
            } else {
                updateStopLoss(dealData.getPrice());
                System.out.printf("hold: %s %.2f, profit/loss: %.2f (%.2f%%), current price: %.2f, stop loss: %.2f%n",
                        currentDeal.getDirection(), currentDeal.getOpenedData().getPrice(),
                        profitLoss, profitLossPercentage * 100, dealData.getPrice(), currentDeal.getStopLoss());
            }
        }
    }

    private static String convertToDateTime(DealData dealData) {
        // Convert the timestamp to a human-readable date-time format
        long when = dealData.getWhen();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(when));
    }

    private double calculateDefaultStopLoss(double price, boolean isLong) {
        double stopLossPercentage = maximumLoss; // Maximum loss percentage
        if (isLong) {
            return price * (1 - stopLossPercentage);
        } else {
            return price * (1 + stopLossPercentage);
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
        if (profitLoss > 0) {
            sumProfit += profitLoss;
        } else {
            sumLoss += profitLoss;
        }
        long openDuration = (dealData.getWhen() - currentDeal.getOpenedData().getWhen()) / 1000L / 60L;  // Duration in minutes
        String durationMessage = String.format("Open duration: %d min", openDuration);

        currentDeal.setMessage(String.format("[DEAL] Deal closed. Reason: %s. PL: %.2f. New balance: %.2f. %s  " + convertToDateTime(dealData),
                reason, profitLoss, balance, durationMessage));
        System.out.println(currentDeal.getMessage());

        closedDeals.add(currentDeal);
        currentDeal = null;
    }

    public double calculateWinRate() {
        if (closedDeals.isEmpty()) {
            System.out.printf("[DEAL] Positive. No closed deals available to calculate win rate. final amount: " + balance);
            return 0.0;
        }

        long wins = closedDeals.stream().filter(deal -> deal.getClosedAmount() > deal.getOpenAmount()).count();
        long losses = closedDeals.size() - wins;

        double winPercentage = (double) wins / closedDeals.size() * 100;
        System.out.printf("[DEAL] Positive P/L deals: %d, Negative P/L deals: %d, winPercentage " + winPercentage + " final amount: " + balance + " sumloss " + sumLoss + " sum profit " + sumProfit, wins, losses);
        return winPercentage;
    }
}
