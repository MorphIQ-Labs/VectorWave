package com.morphiqlabs.wavelet.realworld;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/**
 * Utility class for loading tick data from compressed archives.
 */
public class TickDataLoader {
    
    /**
     * Represents a single tick in the market.
     */
    public record Tick(
        long timestamp,
        Side side,
        int volume,
        double price
    ) {
        public enum Side {
            BID, ASK
        }
    }
    
    /**
     * Loads tick data from the test resources tar.xz file.
     *
     * @return List of ticks
     * @throws IOException if unable to read the file
     */
    @SuppressWarnings("deprecation")  // getNextTarEntry() is still the correct method for TarArchiveInputStream
    public static List<Tick> loadTickData() throws IOException {
        String resourcePath = "/ticks_1734964200000L.tar.xz";
        
        try (InputStream resourceStream = TickDataLoader.class.getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            
            // Create XZ decompressor
            try (XZCompressorInputStream xzStream = new XZCompressorInputStream(resourceStream);
                 TarArchiveInputStream tarStream = new TarArchiveInputStream(xzStream)) {
                
                TarArchiveEntry entry;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    if (!entry.isDirectory()) {
                        // Read the CSV data from the tar entry
                        return parseCsv(tarStream);
                    }
                }
                
                throw new IOException("No valid entries found in tar.xz file");
            }
        } catch (Exception e) {
            // Fallback to system command if Commons Compress fails
            return loadTickDataFallback(resourcePath);
        }
    }
    
    /**
     * Fallback method using system tar command for platforms where Commons Compress might fail.
     */
    private static List<Tick> loadTickDataFallback(String resourcePath) throws IOException {
        try {
            Process process = new ProcessBuilder(
                "tar", "-xOf", 
                TickDataLoader.class.getResource(resourcePath).getPath()
            ).start();
            
            try (InputStream is = process.getInputStream()) {
                List<Tick> ticks = parseCsv(is);
                process.waitFor();
                return ticks;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while loading tick data", e);
        } catch (Exception e) {
            throw new IOException("Failed to load tick data using both Commons Compress and system tar command", e);
        }
    }
    
    /**
     * Parses CSV data from input stream.
     */
    private static List<Tick> parseCsv(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            return reader.lines()
                .filter(line -> !line.trim().isEmpty())
                .map(TickDataLoader::parseLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Parses a single CSV line into a Tick.
     */
    private static Tick parseLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 4) {
                return null;
            }
            
            long timestamp = Long.parseLong(parts[0]);
            Tick.Side side = Tick.Side.valueOf(parts[1]);
            int volume = Integer.parseInt(parts[2]);
            double price = Double.parseDouble(parts[3]);
            
            return new Tick(timestamp, side, volume, price);
        } catch (Exception e) {
            // Skip malformed lines
            return null;
        }
    }
    
    /**
     * Extracts price series from ticks.
     *
     * @param ticks List of ticks
     * @param side Filter by side (null for all)
     * @return Array of prices
     */
    public static double[] extractPrices(List<Tick> ticks, Tick.Side side) {
        return ticks.stream()
            .filter(t -> side == null || t.side() == side)
            .mapToDouble(Tick::price)
            .toArray();
    }
    
    /**
     * Extracts volume-weighted prices from ticks.
     *
     * @param ticks List of ticks
     * @return Array of volume-weighted prices
     */
    public static double[] extractVolumeWeightedPrices(List<Tick> ticks) {
        return ticks.stream()
            .mapToDouble(t -> t.price() * t.volume())
            .toArray();
    }
    
    /**
     * Calculates bid-ask spread from ticks.
     *
     * @param ticks List of ticks
     * @param windowSize Number of ticks to consider for spread
     * @return Array of spreads
     */
    public static double[] calculateSpreads(List<Tick> ticks, int windowSize) {
        List<Double> spreads = new ArrayList<>();
        
        for (int i = 0; i < ticks.size() - windowSize; i++) {
            double maxBid = Double.MIN_VALUE;
            double minAsk = Double.MAX_VALUE;
            
            for (int j = i; j < i + windowSize; j++) {
                Tick tick = ticks.get(j);
                if (tick.side() == Tick.Side.BID) {
                    maxBid = Math.max(maxBid, tick.price());
                } else {
                    minAsk = Math.min(minAsk, tick.price());
                }
            }
            
            if (maxBid != Double.MIN_VALUE && minAsk != Double.MAX_VALUE) {
                spreads.add(minAsk - maxBid);
            }
        }
        
        return spreads.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Aggregates tick data into OHLC bars.
     *
     * @param ticks List of ticks
     * @param barSizeMs Bar size in milliseconds
     * @return List of OHLC bars
     */
    public static List<OHLCBar> aggregateToOHLC(List<Tick> ticks, long barSizeMs) {
        List<OHLCBar> bars = new ArrayList<>();
        
        if (ticks.isEmpty()) {
            return bars;
        }
        
        long currentBarStart = ticks.get(0).timestamp() / barSizeMs * barSizeMs;
        double open = ticks.get(0).price();
        double high = open;
        double low = open;
        double close = open;
        long volume = 0;
        
        for (Tick tick : ticks) {
            long barStart = tick.timestamp() / barSizeMs * barSizeMs;
            
            if (barStart > currentBarStart) {
                // Save current bar
                bars.add(new OHLCBar(currentBarStart, open, high, low, close, volume));
                
                // Start new bar
                currentBarStart = barStart;
                open = tick.price();
                high = open;
                low = open;
                close = open;
                volume = tick.volume();
            } else {
                // Update current bar
                high = Math.max(high, tick.price());
                low = Math.min(low, tick.price());
                close = tick.price();
                volume += tick.volume();
            }
        }
        
        // Save last bar
        bars.add(new OHLCBar(currentBarStart, open, high, low, close, volume));
        
        return bars;
    }
    
    /**
     * Represents an OHLC (Open-High-Low-Close) bar.
     */
    public record OHLCBar(
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        long volume
    ) {
        public double[] toArray() {
            return new double[] { open, high, low, close };
        }
    }
}