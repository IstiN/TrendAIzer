package com.github.istin.tradingaizer.indicator;

public enum Timeframe {
    M1(1),   // 1-minute
    M5(5),   // 5-minute
    M15(15), // 15-minute
    M30(30), // 30-minute
    H1(60),  // 1-hour
    H4(240); // 4-hour

    private final int minutes;

    Timeframe(int minutes) {
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }
}