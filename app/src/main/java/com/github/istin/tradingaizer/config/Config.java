package com.github.istin.tradingaizer.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Config {
    private String apiKey;
    private String apiSecret;
}
