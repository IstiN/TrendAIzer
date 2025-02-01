package com.github.istin.tradingaizer.trader;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.DecisionReason;
import com.github.istin.tradingaizer.utils.DateUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Trader {

    private final String ticker;
    private double sumLoss;
    private double sumProfit;
    @Getter
    private double balance;
    private double maximumLoss;
    private double minimumProfit;
    private double riskPercentage;

    private final DealExecutor dealExecutor;
    @Getter
    private List<Deal> closedDeals = new ArrayList<>();

    private Deal currentDeal;

    public Trader(String ticker, double maximumLoss, double minimumProfit, double riskPercentage, DealExecutor dealExecutor) {
        this.balance = dealExecutor.getBalance();
        this.maximumLoss = maximumLoss;
        this.minimumProfit = minimumProfit;
        this.riskPercentage = riskPercentage;
        this.dealExecutor = dealExecutor;
        this.ticker = ticker;
        this.currentDeal = dealExecutor.getCurrentDeal(ticker);
        if (this.currentDeal != null) {
            System.out.printf("Existing deal loaded: %s at %.2f with trade size %.2f. Stop loss: %.2f%n",
                    currentDeal.getDirection(), currentDeal.getOpenedData().getPrice(),
                    currentDeal.getOpenAmountUSDT(), currentDeal.getStopLoss());
        }
    }

    public void decisionTrigger(String ticker, DecisionReason decisionReason, DealData dealData) {
        Decision decision = decisionReason.getDecision();
        if (currentDeal == null) {
            if (decision == Decision.LONG || decision == Decision.SHORT) {
                double tradeSize = balance * riskPercentage;
                double stopLossPrice = recalcStopLoss(decisionReason, dealData, decision);
                currentDeal = new Deal(ticker, dealData,
                        decision == Decision.LONG ? Direction.LONG : Direction.SHORT, tradeSize);
                currentDeal.setStopLoss(stopLossPrice);
                dealExecutor.submitDeal(currentDeal);
                System.out.printf("[DEAL] New deal opened: %s at %.2f with trade size %.2f. Stop loss: %.2f. Reason: %s   %s%n",
                        currentDeal.getDirection(),
                        dealData.getPrice(),
                        tradeSize,
                        currentDeal.getStopLoss(),
                        decisionReason.getReason(),
                        DateUtils.convertToDateTime(dealData));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                currentDeal = dealExecutor.getCurrentDeal(ticker);
                stopLossPrice = recalcStopLoss(decisionReason, dealData, decision);
                currentDeal.setStopLoss(stopLossPrice);
                dealExecutor.updateStopLoss(currentDeal, stopLossPrice);
            }
            return;
        }
        double profitLoss = calculateProfitLoss(dealData.getPrice());
        double profitLossPercentage = profitLoss / balance;
        if (isStopLossTriggered(dealData.getPrice())) {
            closeDeal(dealData, "Stop loss triggered");
        } else if (profitLossPercentage >= minimumProfit ||
                (decisionReason.getTakeProfit() != null && ((currentDeal.getDirection() == Direction.LONG && dealData.getPrice() >= decisionReason.getTakeProfit())
                        || (currentDeal.getDirection() == Direction.SHORT && dealData.getPrice() <= decisionReason.getTakeProfit())))) {
            closeDeal(dealData, "Take-profit reached");
        } else if (decision == Decision.CLOSE) {
            closeDeal(dealData, "Close decision received: " + decisionReason.getReason());
        } else if ((currentDeal.getDirection() == Direction.LONG && decision == Decision.SHORT) ||
                (currentDeal.getDirection() == Direction.SHORT && decision == Decision.LONG)) {
            closeDeal(dealData, "Opposite signal: " + decisionReason.getReason());
        } else {
            updateStopLoss(dealData.getPrice(), currentDeal.getOpenAmountUSDT());
            System.out.printf("Hold %s @ %.2f, P/L: %.2f (%.2f%%), price: %.2f, stop: %.2f%n",
                    currentDeal.getDirection(),
                    currentDeal.getOpenedData().getPrice(),
                    profitLoss,
                    profitLossPercentage * 100,
                    dealData.getPrice(),
                    currentDeal.getStopLoss());
        }
    }

    private double recalcStopLoss(DecisionReason decisionReason, DealData dealData, Decision decision) {
        return decisionReason.getDynamicStopLoss() != null
                ? decisionReason.getDynamicStopLoss()
                : calculateDefaultStopLoss(dealData.getPrice(), decision == Decision.LONG);
    }

    private double calculateDefaultStopLoss(double price, boolean isLong) {
        if (isLong) {
            return price * (1 - maximumLoss);
        } else {
            return price * (1 + maximumLoss);
        }
    }

    private double calculateProfitLoss(double currentPrice) {
        if (currentDeal.getDirection() == Direction.LONG) {
            return currentDeal.getOpenAmountUSDT() * (currentPrice - currentDeal.getOpenedData().getPrice()) / currentDeal.getOpenedData().getPrice();
        } else {
            return currentDeal.getOpenAmountUSDT() * (currentDeal.getOpenedData().getPrice() - currentPrice) / currentDeal.getOpenedData().getPrice();
        }
    }

    private boolean isStopLossTriggered(double currentPrice) {
        return (currentDeal.getDirection() == Direction.LONG && currentPrice <= currentDeal.getStopLoss()) ||
                (currentDeal.getDirection() == Direction.SHORT && currentPrice >= currentDeal.getStopLoss());
    }

    private void updateStopLoss(double price, double money) {
        double profitPercentage = calculateProfitLoss(price) / money;
        if (profitPercentage > 0) {
            double newStopLoss;
            if (currentDeal.getDirection() == Direction.LONG) {
                newStopLoss = price * (1 - maximumLoss / 2.0);
                currentDeal.setStopLoss(Math.max(currentDeal.getStopLoss(), newStopLoss));
            } else {
                newStopLoss = price * (1 + maximumLoss / 2.0);
                currentDeal.setStopLoss(Math.min(currentDeal.getStopLoss(), newStopLoss));
            }
            dealExecutor.updateStopLoss(currentDeal, currentDeal.getStopLoss());
            System.out.printf("Stop loss updated to %.2f%n", currentDeal.getStopLoss());
        }
    }

    private void closeDeal(DealData dealData, String reason) {
        double profitLoss = calculateProfitLoss(dealData.getPrice());
        currentDeal.setClosedAmount(currentDeal.getOpenAmountUSDT() + profitLoss);
        currentDeal.setCloseData(dealData);
        dealExecutor.closeDeal(currentDeal, dealData.getPrice());
        double oldBalance = this.balance;
        if (dealExecutor instanceof  FakeDealExecutor) {
            ((FakeDealExecutor)dealExecutor).setBalance(balance + profitLoss);
        }
        this.balance = dealExecutor.getBalance();
        double approximatePnL = this.balance - oldBalance;
        if (approximatePnL > 0) {
            sumProfit += approximatePnL;
        } else {
            sumLoss += approximatePnL;
        }
        long openDuration = (dealData.getWhen() - currentDeal.getOpenedData().getWhen()) / 1000L / 60L;
        currentDeal.setMessage(String.format(
                "[DEAL] Deal closed. Reason: %s. ApproxPL: %.2f. OldBalance: %.2f -> NewBalance: %.2f. Duration: %d min.  %s",
                reason,
                approximatePnL,
                oldBalance,
                this.balance,
                openDuration,
                DateUtils.convertToDateTime(dealData))
        );
        System.out.println(currentDeal.getMessage());
        closedDeals.add(currentDeal);
        currentDeal = null;
    }

    public double calculateWinRate() {
        if (closedDeals.isEmpty()) {
            System.out.println("[DEAL] No closed deals, cannot calculate win rate. Final: " + balance);
            return 0.0;
        }
        long wins = closedDeals.stream()
                .filter(deal -> deal.getClosedAmount() > deal.getOpenAmountUSDT())
                .count();
        long losses = closedDeals.size() - wins;
        double winPercentage = (double) wins / closedDeals.size() * 100.0;
        System.out.printf("[DEAL] Wins: %d, Losses: %d, WinRate: %.2f%%, FinalBal: %.2f, SumLoss: %.2f, SumProfit: %.2f%n",
                wins, losses, winPercentage, balance, sumLoss, sumProfit);
        return winPercentage;
    }
}
