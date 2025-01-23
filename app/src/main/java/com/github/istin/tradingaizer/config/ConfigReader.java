package com.github.istin.tradingaizer.config;

import com.github.istin.tradingaizer.App;
import lombok.Getter;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    @Getter
    private Config config;

    public void loadConfig() {
        Properties properties = new Properties();

        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            properties.load(input);

            String apiKey = properties.getProperty("api.key");
            String apiSecret = properties.getProperty("api.secret");

            if (apiKey == null || apiSecret == null) {
                throw new IllegalArgumentException("API key or secret not found in properties file.");
            }
            this.config = new Config(apiKey, apiSecret);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
