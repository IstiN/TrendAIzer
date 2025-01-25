package com.github.istin.tradingaizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DecisionReason {

    private Decision decision;
    private String reason;

}
