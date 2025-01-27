package com.github.istin.tradingaizer.provider;

import com.github.istin.tradingaizer.config.ConfigReader;
import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.trader.StatDealData;
import com.github.istin.tradingaizer.utils.CacheManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BybitDataProvider implements DataProvider {

    private final String apiKey;
    private final String apiSecret;
    private final boolean isBybitCache;
    private final CacheManager cacheManager;

    // OkHttp client instance
    private final OkHttpClient httpClient;

    public BybitDataProvider(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        // Suppose there's a "bybitCache" flag in your config
        this.isBybitCache = new ConfigReader().getConfig().getBybitCache();
        this.cacheManager = new CacheManager("bybit");
        this.httpClient = new OkHttpClient(); // Create a single OkHttpClient instance
    }

    @Override
    public List<? extends StatDealData> fetchHistoricalData(String symbol, String interval, long startTime, long endTime) {
        List<KlineData> klineDataList = new ArrayList<>();
        long currentStartTime = startTime;

        // Loop over the time range, respecting Bybit’s possible limit (e.g., 1000)
        while (currentStartTime < endTime) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("symbol", symbol);
            parameters.put("interval", 1);
            parameters.put("limit", 1000);
            parameters.put("page", 1);
            parameters.put("category", "spot");
            parameters.put("start", currentStartTime);
            parameters.put("end", endTime);

            String response = getResponseFromCacheOrAPI(parameters);
            System.out.println(response);
            try {
                JSONObject jsonObj = new JSONObject(response);
                // Bybit’s success code is often 0
                int retCode = jsonObj.optInt("retCode", -1);
                if (retCode != 0) {
                    System.err.println("Bybit returned error code: " + retCode);
                    break;
                }

                JSONArray resultArray = jsonObj.getJSONObject("result").getJSONArray("list");
                if (resultArray == null || resultArray.length() == 0) {
                    System.out.println("No more kline data from Bybit.");
                    break;
                }

                for (int i = 0; i < resultArray.length(); i++) {
                    JSONArray klineArray = resultArray.getJSONArray(i);

                    // Bybit Spot kline data typically: [ openTime, open, high, low, close, volume ]
                    long openTimeVal      = Long.parseLong(klineArray.getString(0));
                    double openPriceVal   = Double.parseDouble(klineArray.getString(1));
                    double highPriceVal   = Double.parseDouble(klineArray.getString(2));
                    double lowPriceVal    = Double.parseDouble(klineArray.getString(3));
                    double closePriceVal  = Double.parseDouble(klineArray.getString(4));
                    double volumeVal      = Double.parseDouble(klineArray.getString(5));

                    // NOTE: Bybit may provide openTime in seconds or ms; check docs to confirm.
                    // If it's in seconds, you might do: openTimeVal *= 1000;
                    // If it's already ms, just keep as is.

                    KlineData klineData = new KlineData();
                    klineData.setOpenTime(openTimeVal);
                    klineData.setOpenPrice(openPriceVal);
                    klineData.setHighPrice(highPriceVal);
                    klineData.setLowPrice(lowPriceVal);
                    klineData.setClosePrice(closePriceVal);
                    klineData.setVolume(volumeVal);

                    // Bybit typically does not return "closeTime" for spot klines.
                    // You might approximate closeTime = openTime + intervalMillis
                    long assumedCloseTime = openTimeVal + parseIntervalToMillis(interval);
                    klineData.setCloseTime(assumedCloseTime);

                    klineDataList.add(klineData);
                }

                // Move our currentStartTime forward to avoid duplicates
                currentStartTime = klineDataList.get(klineDataList.size() - 1).getOpenTime() + 1;

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error parsing JSON response from Bybit.");
                break;
            }
        }

        return klineDataList;
    }

    /**
     * Convert Bybit interval (e.g., "1", "3", "5", "15", "30", "60", etc.) to milliseconds.
     * Adjust to your usage. If "D" or "W" are possible, handle accordingly.
     */
    private long parseIntervalToMillis(String interval) {
        // Example: "1" => 1 min => 60_000 ms
        //          "60" => 60 min => 3_600_000 ms
        try {
            int i = Integer.parseInt(interval);
            return i * 60_000L;
        } catch (NumberFormatException e) {
            // If there's another format like "D" or "W", handle here
            // For example: "D" => 24*60*60*1000 = 86400000
            return 60_000; // fallback, or throw an exception
        }
    }

    /**
     * Decide whether to read from cache or fetch from Bybit.
     */
    private String getResponseFromCacheOrAPI(Map<String, Object> parameters) {
        String cacheFileName = generateCacheFileName(parameters);

        if (isBybitCache) {
            // Attempt to read from cache
            String cachedResponse = cacheManager.readFromCache(cacheFileName);
            if (cachedResponse != null) {
                return cachedResponse;
            }
            // Otherwise, fetch from Bybit and cache
            String response = fetchKlinesFromBybit(parameters);
            cacheManager.writeToCache(cacheFileName, response);
            return response;
        } else {
            // Directly fetch from Bybit
            return fetchKlinesFromBybit(parameters);
        }
    }

    /**
     * Build a URL and use OkHttp to fetch the data from Bybit.
     * If Bybit requires an API key or signature, add them here.
     */
    private String fetchKlinesFromBybit(Map<String, Object> parameters) {
        // Example endpoint for Spot: https://api.bybit.com/spot/quote/v1/kline
        // Adjust to your environment or testnet endpoint if needed
        String url = "https://api.bybit.com/v5/market/kline";

        // Build query string
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            queryString.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }
        // Remove trailing '&'
        if (queryString.length() > 0) {
            queryString.setLength(queryString.length() - 1);
        }

        String finalUrl = url + "?" + queryString;

        // If Bybit requires signing, you'd do so here, adding api_key, timestamp, sign, etc.
        // For public spot klines, you might not need it. This example is a simple GET.

        Request request = new Request.Builder()
                .url(finalUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Error: HTTP " + response.code() + " - " + response.message());
            }
            return response.body() != null ? response.body().string() : "{}";
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Generate a cache file name based on request parameters.
     */
    private String generateCacheFileName(Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        String paramString = sb.toString();
        return CacheManager.hash(paramString) + ".json";
    }
}
