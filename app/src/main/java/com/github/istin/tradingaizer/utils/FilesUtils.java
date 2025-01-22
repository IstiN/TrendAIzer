package com.github.istin.tradingaizer.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesUtils {
    public static String readFromCache(String fileName) {
        Path filePath = Paths.get(HashUtils.CACHE_FOLDER, fileName);
        try {
            if (Files.exists(filePath)) {
                return new String(Files.readAllBytes(filePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeToCache(String fileName, String content) {
        Path filePath = Paths.get(HashUtils.CACHE_FOLDER, fileName);
        try {
            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createCacheFolder() {
        File cacheDir = new File(HashUtils.CACHE_FOLDER);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
    }
}
