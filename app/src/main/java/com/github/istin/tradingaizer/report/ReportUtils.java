package com.github.istin.tradingaizer.report;

import com.github.istin.tradingaizer.StrategyTestingApp;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.trader.Deal;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportUtils {
    public static void generateReport(String fileName, List<Deal> closedDeals, List<KlineData> historicalData) {
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (Deal deal : closedDeals) {
            Map<String, Object> decisionData = new HashMap<>();
            decisionData.put("action", deal.getDirection());
            decisionData.put("data", deal.getOpenedData());
            decisionData.put("message", "open");
            decisions.add(decisionData);

            Map<String, Object> closedDesitionData = new HashMap<>();
            closedDesitionData.put("action", "CLOSE");
            closedDesitionData.put("data", deal.getCloseData());
            closedDesitionData.put("message", deal.getMessage() == null ? "Still open" : deal.getMessage());
            decisions.add(closedDesitionData);
        }

        // Create FreeMarker Configuration instance
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassForTemplateLoading(StrategyTestingApp.class, "/");
        cfg.setDefaultEncoding("UTF-8");

        try {
            Template template = cfg.getTemplate("chartTemplate.html");

            Map<String, Object> root = new HashMap<>();
            root.put("klineData", historicalData);
            root.put("decisions", decisions);

            // Write to HTML file
            try (Writer fileWriter = new FileWriter(fileName)) {
                template.process(root, fileWriter);
            }

            System.out.println("HTML report generated successfully.");

        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }
}
