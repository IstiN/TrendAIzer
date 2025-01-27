package com.github.istin.tradingaizer.utils;

import com.github.istin.tradingaizer.trader.DealData;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static String convertToDateTime(DealData dealData) {
        // Convert the timestamp to a human-readable date-time format
        long when = dealData.getWhen();
        return covertToDateTime(when);
    }

    @NotNull
    public static String covertToDateTime(long when) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(when));
    }
}
