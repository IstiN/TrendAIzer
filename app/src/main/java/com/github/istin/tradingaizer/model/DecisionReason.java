package com.github.istin.tradingaizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DecisionReason {

    private Decision decision;
    private String reason;
    private Double dynamicStopLoss; // New parameter for dynamic stop-loss
    private Double takeProfit; // Optional: Take-profit parameter (can be null)

    // Constructor for backward compatibility (if stopLoss/takeProfit are not provided)
    public DecisionReason(Decision decision, String reason) {
        this.decision = decision;
        this.reason = reason;
        this.dynamicStopLoss = null;
        this.takeProfit = null;
    }
}
