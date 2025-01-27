package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.trader.StatData;

import java.util.ArrayList;
import java.util.List;

public class ADXIndicator extends Indicator<Double> {

    private final int period; // Typically 14

    public ADXIndicator(int period) {
        this.period = period;
    }

    @Override
    public Double calculate(List<? extends StatData> historicalData) {
        /*
         * The ADX calculation requires enough data to compute
         * directional movement and true ranges over a 'period'.
         */
        if (historicalData.size() < period + 1) {
            System.out.println("Not enough data to calculate ADX. Required: " + (period + 1)
                    + ", but got: " + historicalData.size());
            return null;
        }

        // Step 1: Prepare arrays for True Range (TR), +DM, -DM
        int size = historicalData.size();
        double[] trueRange = new double[size];
        double[] plusDM = new double[size];
        double[] minusDM = new double[size];

        // Calculate initial values
        for (int i = 1; i < size; i++) {
            StatData statData = historicalData.get(i);
            double currentHigh  = statData.getHighPrice();
            double currentLow   = statData.getLowPrice();
            StatData prevStatData = historicalData.get(i - 1);
            double prevClose    = prevStatData.getClosePrice();
            double prevHigh     = prevStatData.getHighPrice();
            double prevLow      = prevStatData.getLowPrice();

            // True Range
            double highMinusLow   = currentHigh - currentLow;
            double highMinusClose = Math.abs(currentHigh - prevClose);
            double lowMinusClose  = Math.abs(currentLow - prevClose);
            trueRange[i] = Math.max(highMinusLow, Math.max(highMinusClose, lowMinusClose));

            // +DM and -DM
            double upMove   = currentHigh - prevHigh;
            double downMove = prevLow - currentLow;

            if (upMove > downMove && upMove > 0) {
                plusDM[i] = upMove;
            } else {
                plusDM[i] = 0;
            }

            if (downMove > upMove && downMove > 0) {
                minusDM[i] = downMove;
            } else {
                minusDM[i] = 0;
            }
        }

        // Step 2: Apply a Wilder's smoothing for TR, +DM, -DM
        // Initialize first smoothed values by summation of the first 'period' points
        double atr14 = 0.0;
        double plusDM14 = 0.0;
        double minusDM14 = 0.0;

        for (int i = 1; i <= period; i++) {
            atr14     += trueRange[i];
            plusDM14  += plusDM[i];
            minusDM14 += minusDM[i];
        }

        // The first smoothed values
        double prevATR   = atr14;
        double prevPlusDM  = plusDM14;
        double prevMinusDM = minusDM14;

        // Arrays to store smoothed TR, +DM, -DM
        double[] smoothedATR     = new double[size];
        double[] smoothedPlusDM  = new double[size];
        double[] smoothedMinusDM = new double[size];

        smoothedATR[period]     = prevATR;
        smoothedPlusDM[period]  = prevPlusDM;
        smoothedMinusDM[period] = prevMinusDM;

        // Step 3: Smooth for subsequent bars
        for (int i = period + 1; i < size; i++) {
            // Smooth the TR, +DM, -DM using Wilder's averaging
            prevATR      = (prevATR - (prevATR / period))     + trueRange[i];
            prevPlusDM   = (prevPlusDM - (prevPlusDM / period)) + plusDM[i];
            prevMinusDM  = (prevMinusDM - (prevMinusDM / period)) + minusDM[i];

            smoothedATR[i]     = prevATR;
            smoothedPlusDM[i]  = prevPlusDM;
            smoothedMinusDM[i] = prevMinusDM;
        }

        /*
         * Step 4: Calculate +DI, -DI, and DX for each bar starting from 'period' index
         */
        double[] plusDI = new double[size];
        double[] minusDI = new double[size];
        double[] dx      = new double[size];

        for (int i = period; i < size; i++) {
            if (smoothedATR[i] == 0) {
                plusDI[i]  = 0;
                minusDI[i] = 0;
            } else {
                plusDI[i]  = (smoothedPlusDM[i]  / smoothedATR[i]) * 100;
                minusDI[i] = (smoothedMinusDM[i] / smoothedATR[i]) * 100;
            }

            double sumDI = plusDI[i] + minusDI[i];
            double diffDI = Math.abs(plusDI[i] - minusDI[i]);
            if (sumDI == 0) {
                dx[i] = 0;
            } else {
                dx[i] = (diffDI / sumDI) * 100;
            }
        }

        /*
         * Step 5: Smooth the DX values to get ADX
         * We can do a Wilder's smoothing on the DX array as well.
         */
        double adx = 0.0;
        // First ADX is average of the first 'period' dx values (starting from period, next 'period' bars)
        int adxStartIndex = period * 2; // Because we need at least 'period' for the first +DI/-DI,
        // and another 'period' to start averaging DX typically.

        if (adxStartIndex > size) {
            // Not enough data to produce a final ADX
            return null;
        }

        // Sum of the first 'period' dx values after adxStartIndex - period
        double sumDX = 0.0;
        for (int i = period; i < period * 2; i++) {
            sumDX += dx[i];
        }
        // The initial ADX
        adx = sumDX / period;

        // Then continue smoothing for subsequent bars
        for (int i = period * 2; i < size; i++) {
            adx = ((adx * (period - 1)) + dx[i]) / period;
        }

        return adx; // Return the last ADX value
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " period=" + period;
    }
}
