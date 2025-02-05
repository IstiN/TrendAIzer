package com.github.istin.tradingaizer.provider;

import com.binance.connector.client.impl.SpotClientImpl;
import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.trader.StatDealData;
import com.github.istin.tradingaizer.utils.CacheManager;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinanceDataProvider implements DataProvider {

    private final boolean isBinanceCache;
    private final SpotClientImpl client;
    private final CacheManager cacheManager;

    public BinanceDataProvider(String apiKey, String apiSecret) {
        this.client = new SpotClientImpl(apiKey, apiSecret, "https://api.binance.com");
        this.isBinanceCache = new ConfigReader().getConfig().getBinanceCache();
        this.cacheManager = new CacheManager("binance");
    }

    @Override
    public List<? extends StatDealData> fetchHistoricalData(String symbol, String interval, long startTime, long endTime) {
        List<KlineData> klineDataList = new ArrayList<>();
        long currentStartTime = startTime;

        while (currentStartTime < endTime) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("symbol", symbol);
            parameters.put("interval", interval);
            parameters.put("startTime", currentStartTime);
            parameters.put("endTime", endTime);
            parameters.put("limit", 1000); // Binance API limit

            String response = getResponseFromCacheOrAPI(parameters);

            try {
                JSONArray jsonArray = new JSONArray(response);
                if (jsonArray.length() == 0) {
                    break; // No more data
                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONArray klineArray = jsonArray.getJSONArray(i);
                    KlineData klineData = new KlineData();
                    klineData.setOpenTime(klineArray.getLong(0));
                    klineData.setOpenPrice(klineArray.getDouble(1));
                    klineData.setHighPrice(klineArray.getDouble(2));
                    klineData.setLowPrice(klineArray.getDouble(3));
                    klineData.setClosePrice(klineArray.getDouble(4));
                    klineData.setVolume(klineArray.getDouble(5));
                    klineData.setCloseTime(klineArray.getLong(6));

                    klineDataList.add(klineData);
                }

                currentStartTime = klineDataList.get(klineDataList.size() - 1).getCloseTime() + 1;

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error parsing JSON response from Binance.");
                break;
            }
        }
        return klineDataList;
    }

    private String getResponseFromCacheOrAPI(Map<String, Object> parameters) {
        String cacheFileName = generateCacheFileName(parameters);
        if (isBinanceCache) {
            String cachedResponse = cacheManager.readFromCache(cacheFileName);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            String response = client.createMarket().klines(parameters);
            cacheManager.writeToCache(cacheFileName, response);
            return response;
        } else {
            return client.createMarket().klines(parameters);
        }
    }

    private String generateCacheFileName(Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        String paramString = sb.toString();
        return CacheManager.hash(paramString) + ".json";
    }
}