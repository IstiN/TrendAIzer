package com.github.istin.tradingaizer.parser;

import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.trader.Deal;
import com.github.istin.tradingaizer.trader.Direction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
public class DealParser {

    public List<Deal> parseDealsFromResource(String ticker, String resourcePath) {
        List<Deal> deals = new ArrayList<>();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = line.split(";");
                if (fields.length < 18) { // Adjust to 18 fields
                    System.err.println("Skipping invalid line (fewer than 18 fields): " + line);
                    continue;
                }

                if (fields.length == 18) {
                    // Log: Missing last field
                    System.err.println("Detected 18 fields (missing optional 'message'): " + line);
                }

                try {
                    // Parse fields
                    Direction direction = parseDirection(fields[2]);
                    double stopLoss = parseDouble(fields[15], "stopLoss");
                    double openAmount = parseDouble(fields[6], "openAmount");
                    double closedAmount = parseDouble(fields[7], "closedAmount");
                    String message = fields.length > 18 ? fields[18] : "";

                    // Parse timestamps
                    long openTime = parseTimestamp(fields[11]);
                    long closeTime = parseTimestamp(fields[12]);
                    long dealOpenTime = parseTimestamp(fields[13]);
                    long dealCloseTime = parseTimestamp(fields[14]);

                    // Parse KlineData for opening
                    KlineData openedKlineData = new KlineData();
                    openedKlineData.setOpenTime(dealOpenTime);
//                    openedKlineData.setOpenPrice(parseDouble(fields[7], "openPrice"));
//                    openedKlineData.setHighPrice(parseDouble(fields[8], "highPrice"));
//                    openedKlineData.setLowPrice(parseDouble(fields[9], "lowPrice"));
                    openedKlineData.setClosePrice(parseDouble(fields[8], "openPrice"));
//                    openedKlineData.setVolume(parseDouble(fields[17], "volume"));
//                    openedKlineData.setCloseTime(closeTime);

                    // Parse KlineData for closing
                    KlineData closedKlineData = new KlineData();
                    closedKlineData.setOpenTime(dealCloseTime);
//                    closedKlineData.setOpenPrice(parseDouble(fields[7], "closeOpenPrice"));
//                    closedKlineData.setHighPrice(parseDouble(fields[8], "closeHighPrice"));
//                    closedKlineData.setLowPrice(parseDouble(fields[9], "closeLowPrice"));
                    closedKlineData.setClosePrice(parseDouble(fields[9], "closeLowPrice"));
//                    closedKlineData.setVolume(parseDouble(fields[17], "closeVolume"));
//                    closedKlineData.setCloseTime(dealCloseTime)
                    ;

                    // Create Deal object
                    Deal deal = new Deal(ticker, openedKlineData, closedKlineData, direction, stopLoss, openAmount, closedAmount, message);
                    deals.add(deal);
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deals;
    }

    private Direction parseDirection(String field) {
        try {
            return Direction.valueOf(field.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid direction: " + field, e);
        }
    }

    private double parseDouble(String field, String fieldName) {
        try {
            return Double.parseDouble(field);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid double for " + fieldName + ": " + field, e);
        }
    }

    private long parseTimestamp(String field) {
        try {
            return java.time.Instant.parse(field).toEpochMilli() - 3 * 60 * 60 * 1000;
        } catch (Exception e) {
            throw new RuntimeException("Invalid timestamp: " + field, e);
        }
    }
}
