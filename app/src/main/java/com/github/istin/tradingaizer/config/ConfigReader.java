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

        try (InputStream input = ReportBuildingApp.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            properties.load(input);

            String apiKey = properties.getProperty("BINANCE_KEY");
            String apiSecret = properties.getProperty("BINANCE_SECRET");
            Boolean indicatorCache = Boolean.parseBoolean(properties.getProperty("INDICATOR_CACHE"));
            Boolean binanceCache = Boolean.parseBoolean(properties.getProperty("BINANCE_CACHE"));
            Boolean bybitCache = Boolean.parseBoolean(properties.getProperty("BYBIT_CACHE"));

            if (apiKey == null || apiSecret == null) {
                throw new IllegalArgumentException("API key or secret not found in properties file.");
            }
            this.config = new Config(apiKey, apiSecret, indicatorCache, binanceCache, bybitCache);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
