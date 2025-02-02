package com.github.istin.tradingaizer.trader;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;

public class BinanceDealExecutor implements DealExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BinanceDealExecutor.class);
    private final UMFuturesClientImpl client;

    public BinanceDealExecutor(String apiKey, String secretKey) {
        this.client = new UMFuturesClientImpl(
                apiKey,
                secretKey
        );
    }

    public void getKlineData(String symbol, String interval) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("interval", interval);
        String klines = client.market().klines(parameters);
    }

    @Override
    public double getBalance() {
        String response = client.account().futuresAccountBalance(new LinkedHashMap<>());
        logger.info(response);
        // Parse the JSON response
        JSONArray balances = new JSONArray(response);
        for (int i = 0; i < balances.length(); i++) {
            JSONObject balance = balances.getJSONObject(i);
            if (balance.getString("asset").equals("USDT")) {
                return balance.getDouble("balance");
            }
        }
        return 0d;
    }

    @Override
    public void submitDeal(Deal deal) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", deal.getTicker());
        parameters.put("side", deal.getDirection() == Direction.LONG ? "BUY" : "SELL");
        parameters.put("type", "MARKET");
        double price = deal.getOpenedData().getPrice();
        double quantity = getQuantity(deal.getTicker(), deal.getOpenAmountUSDT(), price);
        parameters.put("quantity", quantity);
        try {
            String result = client.account().newOrder(parameters);
            logger.info("Deal submitted: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error submitting deal: {}", e.getMessage(), e);
        }
    }

    private double getQuantity(String ticker, double openAmountUSDT, double price) {
        String response = client.market().exchangeInfo();
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray symbols = jsonResponse.getJSONArray("symbols");
        String stepSize = null;
        for (int i = 0; i < symbols.length(); i++) {
            JSONObject symbol = symbols.getJSONObject(i);
            if (symbol.getString("symbol").equals(ticker)) {
                JSONArray filters = symbol.getJSONArray("filters");
                for (int j = 0; j < filters.length(); j++) {
                    JSONObject filter = filters.getJSONObject(j);
                    if (filter.getString("filterType").equals("LOT_SIZE")) {
                        stepSize = filter.getString("stepSize");
                        break;
                    }
                }
                break;
            }
        }
        if (stepSize == null) {
            throw new RuntimeException("Step size not found for BTCUSDT");
        }
        BigDecimal stepSizeDecimal = new BigDecimal(stepSize);
        BigDecimal quantity = BigDecimal.valueOf(openAmountUSDT / price);
        return quantity.setScale(stepSizeDecimal.scale(), RoundingMode.DOWN).doubleValue();
    }

    @Override
    public void closeDeal(Deal deal, double closePrice) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", deal.getTicker());
        parameters.put("side", deal.getDirection() == Direction.LONG ? "SELL" : "BUY");
        parameters.put("type", "MARKET");
        parameters.put("quantity", Math.abs(deal.getOpenedData().getVolume()));
        parameters.put("reduceOnly", "true");
        try {
            String result = client.account().newOrder(parameters);
            logger.info("Deal closed: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error closing deal: {}", e.getMessage(), e);
        }
        client.account().cancelAllOpenOrders(parameters);
    }

    @Override
    public Deal getCurrentDeal(String ticker) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", ticker);
        try {
            String result = client.account().currentAllOpenOrders(parameters);
            logger.info("Fetched opened orders: {}", result);
            Double stopLoss = parseStopLoss(result);
            result = client.account().positionInformation(parameters);
            logger.info("Fetched position information: {}", result);
            Deal currentDeal = parseCurrentDeal(result, stopLoss);
            return currentDeal;
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error fetching current deal: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void updateStopLoss(Deal deal, double newStopLoss) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", deal.getTicker());
        client.account().cancelAllOpenOrders(parameters);
        parameters.put("side", deal.getDirection() == Direction.LONG ? "SELL" : "BUY");
        parameters.put("positionSide", "BOTH");
        // Use deal.getOpenedData().getPrice() instead of fetching current price
        double currentPrice = deal.getOpenedData().getPrice();
        String orderType = decideStopOrderType(deal, newStopLoss, currentPrice);
        parameters.put("type", orderType);
        parameters.put("closePosition", true);
        // Round the newStopLoss to allowed precision for the asset
        double roundedStopLoss = roundPrice(deal.getTicker(), newStopLoss);
        parameters.put("stopPrice", roundedStopLoss);
        try {
            String result = client.account().newOrder(parameters);
            logger.info("Stop loss updated: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error updating stop loss: {}", e.getMessage(), e);
        }
    }

    private String decideStopOrderType(Deal deal, double newStopLoss, double currentPrice) {
        Direction dir = deal.getDirection();
        boolean isLong = (dir == Direction.LONG);
        if (isLong) {
            return (newStopLoss < currentPrice) ? "STOP_MARKET" : "TAKE_PROFIT_MARKET";
        } else {
            return (newStopLoss > currentPrice) ? "STOP_MARKET" : "TAKE_PROFIT_MARKET";
        }
    }

    public static Double parseStopLoss(String jsonString) {
        JSONArray orders = new JSONArray(jsonString);
        for (int i = 0; i < orders.length(); i++) {
            JSONObject order = orders.getJSONObject(i);
            String type = order.optString("type", "");
            if ("STOP_MARKET".equals(type) || "TAKE_PROFIT_MARKET".equals(type)) {
                return order.getDouble("stopPrice");
            }
        }
        return null;
    }

    private Deal parseCurrentDeal(String result, Double stopLossOrderValue) {
        JSONArray positions = new JSONArray(result);
        if (positions.length() == 0) {
            return null;
        }
        for (int i = 0; i < positions.length(); i++) {
            JSONObject position = positions.getJSONObject(i);
            String ticker = position.getString("symbol");
            double openAmount = position.getDouble("positionAmt");
            double entryPrice = position.getDouble("entryPrice");
            double stopLoss = stopLossOrderValue != null ? stopLossOrderValue : 0d;
            if (openAmount == 0d && entryPrice == 0d) {
                return null;
            }
            Direction direction = openAmount > 0 ? Direction.LONG : Direction.SHORT;
            final double openedAmountUSDT = Math.abs(openAmount * entryPrice);
            DealData openedKlineData = new DealData() {
                @Override
                public long getWhen() {
                    return position.getLong("updateTime");
                }
                @Override
                public double getPrice() {
                    return entryPrice;
                }
                @Override
                public double getVolume() {
                    return openAmount;
                }
            };
            Deal deal = new Deal(ticker, openedKlineData, direction, openedAmountUSDT);
            deal.setStopLoss(stopLoss);
            return deal;
        }
        return null;
    }

    private double roundPrice(String ticker, double price) {
        String response = client.market().exchangeInfo();
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray symbols = jsonResponse.getJSONArray("symbols");
        BigDecimal tickSizeBD = null;
        for (int i = 0; i < symbols.length(); i++) {
            JSONObject symbol = symbols.getJSONObject(i);
            if (symbol.getString("symbol").equalsIgnoreCase(ticker)) {
                JSONArray filters = symbol.getJSONArray("filters");
                for (int j = 0; j < filters.length(); j++) {
                    JSONObject filter = filters.getJSONObject(j);
                    if ("PRICE_FILTER".equals(filter.getString("filterType"))) {
                        tickSizeBD = new BigDecimal(filter.getString("tickSize"));
                        break;
                    }
                }
                break;
            }
        }
        if (tickSizeBD == null) {
            throw new RuntimeException("Tick size not found for " + ticker);
        }
        int scale = tickSizeBD.stripTrailingZeros().scale();
        BigDecimal priceBD = BigDecimal.valueOf(price);
        return priceBD.setScale(scale, RoundingMode.DOWN).doubleValue();
    }
}
