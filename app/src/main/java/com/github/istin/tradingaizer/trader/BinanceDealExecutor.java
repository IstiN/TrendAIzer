package com.github.istin.tradingaizer.trader;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public void submitDeal(Deal deal) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", deal.getTicker());
        parameters.put("side", deal.getDirection() == Direction.LONG ? "BUY" : "SELL");
        parameters.put("type", "MARKET");
        //parameters.put("leverage", 1);
        parameters.put("quantity", deal.getOpenAmount());
        //{"code":-1106,"msg":"Parameter 'stopprice' sent when not required."}
        //parameters.put("stopPrice", deal.getStopLoss());

        try {
            String result = client.account().newOrder(parameters);
            logger.info("Deal submitted: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error submitting deal: {}", e.getMessage(), e);
        }
    }

    @Override
    public void closeDeal(Deal deal, double closePrice) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", deal.getTicker());
        parameters.put("side", deal.getDirection() == Direction.LONG ? "SELL" : "BUY");
        parameters.put("type", "MARKET");
        parameters.put("quantity", deal.getOpenAmount());

        try {
            String result = client.account().newOrder(parameters);
            logger.info("Deal closed: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error closing deal: {}", e.getMessage(), e);
        }
    }

    @Override
    public Deal getCurrentDeal() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", "XRPUSDT");

        try {
            String result = client.account().currentAllOpenOrders(parameters);
            logger.info("Fetched opened orders: {}", result);

            //this one is to use to fetch current positions
            result = client.account().positionInformation(parameters);
            logger.info("Fetched position information: {}", result);

            //TODO must be combination from 2 responses, from orders we can take stop loss and take profit
            /// 2025-01-24 12:20:19.758[1737710419758] | INFO  | main       | c.b.c.f.client.utils.RequestHandler  - GET https://fapi.binance.com/fapi/v1/allOrders?symbol=XRPUSDT&timestamp=1737710419726&signature=05d13d12860b94bf8195dd84198ae47988be036d83109451d1677e42c6fd8771
            /// 2025-01-24 12:20:21.360[1737710421360] | INFO  | main       | c.g.i.t.trader.BinanceDealExecutor   - Fetched opened orders: [{"orderId":83896773794,"symbol":"XRPUSDT","status":"NEW","clientOrderId":"ios_rp4eXL7tl3mko36dKzff","price":"0","avgPrice":"0","origQty":"0","executedQty":"0","cumQuote":"0.00000","timeInForce":"GTE_GTC","type":"TAKE_PROFIT_MARKET","reduceOnly":true,"closePosition":true,"side":"SELL","positionSide":"BOTH","stopPrice":"3.5000","workingType":"MARK_PRICE","priceProtect":true,"origType":"TAKE_PROFIT_MARKET","priceMatch":"NONE","selfTradePreventionMode":"EXPIRE_MAKER","goodTillDate":0,"time":1737710403129,"updateTime":1737710403129},{"orderId":83896773799,"symbol":"XRPUSDT","status":"NEW","clientOrderId":"ios_qszWBhVSpCeMs61rtPlF","price":"0","avgPrice":"0","origQty":"0","executedQty":"0","cumQuote":"0.00000","timeInForce":"GTE_GTC","type":"STOP_MARKET","reduceOnly":true,"closePosition":true,"side":"SELL","positionSide":"BOTH","stopPrice":"3.1000","workingType":"MARK_PRICE","priceProtect":true,"origType":"STOP_MARKET","priceMatch":"NONE","selfTradePreventionMode":"EXPIRE_MAKER","goodTillDate":0,"time":1737710403130,"updateTime":1737710403130}]
            /// 2025-01-24 12:20:21.360[1737710421360] | INFO  | main       | c.b.c.f.client.utils.RequestHandler  - GET https://fapi.binance.com/fapi/v2/positionRisk?symbol=XRPUSDT&timestamp=1737710421360&signature=adbbc0adaf720ac441a405b914af64715992da90e7663c886b69465e49a2de0b
            /// 2025-01-24 12:20:21.944[1737710421944] | INFO  | main       | c.g.i.t.trader.BinanceDealExecutor   - Fetched position information: [{"symbol":"XRPUSDT","positionAmt":"2.0","entryPrice":"3.1717","breakEvenPrice":"3.17328585","markPrice":"3.18070000","unRealizedProfit":"0.01800000","liquidationPrice":"0","leverage":"1","maxNotionalValue":"1.0E8","marginType":"cross","isolatedMargin":"0.00000000","isAutoAddMargin":"false","positionSide":"BOTH","notional":"6.36140000","isolatedWallet":"0","updateTime":1737708453714,"isolated":false,"adlQuantile":1}]
            // Logic to find the current open deal (simplified example):
            // Parse the result to find the latest open order matching the criteria
            // For demonstration purposes, we will assume the deal is parsed here:

            Deal currentDeal = parseCurrentDeal(result);
            return currentDeal;

        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error fetching current deal: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void updateStopLoss(Deal deal, double newStopLoss) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", "BTCUSDT");
        parameters.put("side", deal.getDirection() == Direction.LONG ? "SELL" : "BUY");
        parameters.put("type", "STOP_MARKET");
        parameters.put("stopPrice", newStopLoss);
        parameters.put("quantity", deal.getOpenAmount());

        try {
            String result = client.account().newOrder(parameters);
            logger.info("Stop loss updated: {}", result);
        } catch (BinanceConnectorException | BinanceClientException e) {
            logger.error("Error updating stop loss: {}", e.getMessage(), e);
        }
    }

    private Deal parseCurrentDeal(String result) {
        JSONArray positions = new JSONArray(result);
        if (positions.isEmpty()) {
            return null; // No open positions
        }

        JSONObject position = positions.getJSONObject(0); // Assuming single position for now
        String ticker = position.getString("symbol");
        double openAmount = position.getDouble("positionAmt");
        double entryPrice = position.getDouble("entryPrice");
        double stopLoss = position.getDouble("liquidationPrice"); // Approximation for stop loss
        Direction direction = openAmount > 0 ? Direction.LONG : Direction.SHORT;

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

        return new Deal(ticker, stopLoss, openedKlineData, direction, Math.abs(openAmount));
    }
}
