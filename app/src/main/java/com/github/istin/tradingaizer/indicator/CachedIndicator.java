package com.github.istin.tradingaizer.indicator;

import com.github.istin.tradingaizer.model.KlineData;
import com.github.istin.tradingaizer.utils.FilesUtils;
import com.github.istin.tradingaizer.utils.HashUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class CachedIndicator extends Indicator {

    private final String cacheId;

    @Getter
    @Setter
    private Indicator indicator;

    public CachedIndicator(String cacheId) {
        this.cacheId = cacheId;
        FilesUtils.createCacheFolder();
    }

    @Override
    public double calculate(List<KlineData> historicalData) {
        String cacheFileName = HashUtils.getMd5(cacheId + indicator.getClass().getSimpleName() + historicalData.getLast().getOpenTime());
        String cachedResponse = FilesUtils.readFromCache(cacheFileName);

        if (cachedResponse != null) {
            return Double.parseDouble(cachedResponse);
        }

        double result = indicator.calculate(historicalData);
        FilesUtils.writeToCache(cacheFileName, "" + result);
        return result;
    }

}
