package com.github.istin.tradingaizer;

import com.github.istin.tradingaizer.model.Decision;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.provider.BinanceDataProvider;
import com.github.istin.tradingaizer.provider.DataProvider;
import com.github.istin.tradingaizer.strategy.AdvancedStrategy;
import com.github.istin.tradingaizer.strategy.BasicStrategy;
import com.github.istin.tradingaizer.strategy.Strategy;
import com.github.istin.tradingaizer.trader.Trader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;

public class BinanceTradingAlgo {

    private static String apiKey;
    private static String apiSecret;

    private static void loadConfig() {
        Properties properties = new Properties();

        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            properties.load(input);

            apiKey = properties.getProperty("api.key");
            apiSecret = properties.getProperty("api.secret");

            if (apiKey == null || apiSecret == null) {
                throw new IllegalArgumentException("API key or secret not found in properties file.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        loadConfig();

        DataProvider dataProvider = new BinanceDataProvider(apiKey, apiSecret);
        //long endTime = System.currentTimeMillis();
        long endTime = 1737474710146L;
        long threeMonthsAgo = endTime - (1L * 30 * 24 * 60 * 60 * 1000);

        String ticker = "BTCUSDT";
        String interval = "1m";
        String cacheId = ticker + "_" + interval + "_" + endTime + "_" + threeMonthsAgo;

        List<KlineData> historicalData = dataProvider.fetchHistoricalData(ticker, interval, threeMonthsAgo, endTime);

        List<KlineData> timelineSimulation = new ArrayList<>();

        List<Strategy> strategies = new ArrayList<>();

        //strategies.add(new BasicStrategy(cacheId));
        strategies.add(new AdvancedStrategy(cacheId));
        Trader trader = new Trader(1000d, 0.03d, 0.02d, 0.1d);
        for (Strategy strategy : strategies) {

            for (KlineData data : historicalData) {
                timelineSimulation.add(data);
                try {
                    Decision decision = strategy.generateDecision(timelineSimulation);
                    trader.decisionTrigger(decision, data);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

        }

        List<Trader.Deal> closedDeals = trader.getClosedDeals();
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (Trader.Deal deal : closedDeals) {
            Map<String, Object> decisionData = new HashMap<>();
            decisionData.put("action", deal.getDirection());
            decisionData.put("data", deal.getOpenedKlineData());
            decisionData.put("message", "open");
            decisions.add(decisionData);

            Map<String, Object> closedDesitionData = new HashMap<>();
            closedDesitionData.put("action", "close");
            closedDesitionData.put("data", deal.getClosedKlineData());
            closedDesitionData.put("message", deal.getMessage() == null ? "Still open" : deal.getMessage());
            decisions.add(closedDesitionData);
        }
        System.out.println(closedDeals);
        System.out.println(trader.getBalance());

        // Create FreeMarker Configuration instance
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassForTemplateLoading(BinanceTradingAlgo.class, "/");
        cfg.setDefaultEncoding("UTF-8");

        try {
            Template template = cfg.getTemplate("chartTemplate.html");

            Map<String, Object> root = new HashMap<>();
            root.put("klineData", historicalData);
            root.put("decisions", decisions);

            // Write to HTML file
            try (Writer fileWriter = new FileWriter("trading_chart.html")) {
                template.process(root, fileWriter);
            }

            System.out.println("HTML report generated successfully.");

        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }

}
