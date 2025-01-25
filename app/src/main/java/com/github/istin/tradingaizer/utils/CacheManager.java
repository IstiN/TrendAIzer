package com.github.istin.tradingaizer.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CacheManager {

    private final String cacheFolder;

    public CacheManager(String providerName) {
        this.cacheFolder = "cache/" + providerName;
        createCacheFolder();
    }

    private void createCacheFolder() {
        try {
            Files.createDirectories(Paths.get(cacheFolder));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to create cache folder: " + cacheFolder);
        }
    }

    public void writeToCache(String fileName, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFolder + "/" + fileName))) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write to cache: " + fileName);
        }
    }

    public String readFromCache(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFolder + "/" + fileName))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return null; // Cache miss
        }
    }

    public static String hash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash data", e);
        }
    }
}