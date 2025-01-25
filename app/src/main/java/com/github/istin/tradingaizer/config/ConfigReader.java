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

            String apiKey = properties.getProperty("binance.key");
            String apiSecret = properties.getProperty("binance.secret");
            Boolean indicatorCache = Boolean.parseBoolean(properties.getProperty("indicator.cache"));
            Boolean binanceCache = Boolean.parseBoolean(properties.getProperty("binance.cache"));

            if (apiKey == null || apiSecret == null) {
                throw new IllegalArgumentException("API key or secret not found in properties file.");
            }
            this.config = new Config(apiKey, apiSecret, indicatorCache, binanceCache);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
