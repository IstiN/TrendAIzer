package com.github.istin.tradingaizer.config;

import com.github.istin.tradingaizer.ReportBuildingApp;
import lombok.Getter;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    @Getter
    private Config config;

    public ConfigReader() {
        loadConfig();
    }

    public void loadConfig() {
        Properties properties = new Properties();

        // Load properties from file (fallback mechanism)
        try (InputStream input = ReportBuildingApp.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                //System.out.println("WARNING: config.properties not found. Relying on environment variables.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Read from environment variables first, fallback to properties file
        String apiKey = getEnvOrProperty("BINANCE_KEY", properties);
        String apiSecret = getEnvOrProperty("BINANCE_SECRET", properties);
        Boolean indicatorCache = Boolean.parseBoolean(getEnvOrProperty("INDICATOR_CACHE", properties));
        Boolean binanceCache = Boolean.parseBoolean(getEnvOrProperty("BINANCE_CACHE", properties));
        Boolean bybitCache = Boolean.parseBoolean(getEnvOrProperty("BYBIT_CACHE", properties));

        // Validate required variables
        if (apiKey == null || apiSecret == null) {
            throw new IllegalArgumentException("ERROR: API key or secret not found in environment variables or properties file.");
        }

        this.config = new Config(apiKey, apiSecret, indicatorCache, binanceCache, bybitCache);
    }

    private String getEnvOrProperty(String key, Properties properties) {
        String value = System.getenv(key); // Check environment variable first
        if (value == null) {
            value = properties.getProperty(key); // Fallback to properties file
        }
        return value;
    }
}
