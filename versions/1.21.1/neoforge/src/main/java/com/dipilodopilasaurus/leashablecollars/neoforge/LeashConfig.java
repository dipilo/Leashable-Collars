package com.dipilodopilasaurus.leashablecollars.neoforge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class LeashConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeashConfig.class);

    private static double pullFactorBase = 0.25D;
    private static double pullFactorPerLoyalty = 0.05D;
    private static double verticalFactorUp = 0.35D;
    private static double verticalFactorDown = 0.15D;
    private static double minDistanceFloor = 1.5D;
    private static double minDistanceBase = 4.0D;
    private static double maxDistanceBase = 10.0D;

    private LeashConfig() {}

    public static void load() {
        File cfg = new File("config/playercollars-neoforge.properties");
        if (!cfg.exists()) {
            LOGGER.info("No leash config found at {} — creating default config", cfg.getPath());
            Properties defaults = new Properties();
            defaults.setProperty("leash.pullFactorBase", Double.toString(pullFactorBase));
            defaults.setProperty("leash.pullFactorPerLoyalty", Double.toString(pullFactorPerLoyalty));
            defaults.setProperty("leash.verticalFactorUp", Double.toString(verticalFactorUp));
            defaults.setProperty("leash.verticalFactorDown", Double.toString(verticalFactorDown));
            defaults.setProperty("leash.minDistanceFloor", Double.toString(minDistanceFloor));
            defaults.setProperty("leash.minDistanceBase", Double.toString(minDistanceBase));
            defaults.setProperty("leash.maxDistanceBase", Double.toString(maxDistanceBase));
            try {
                File parent = cfg.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileOutputStream out = new FileOutputStream(cfg)) {
                    defaults.store(out, "PlayerCollars NeoForge leash config - edit values and restart the server");
                }
                LOGGER.info("Wrote default leash config to {}", cfg.getPath());
            } catch (IOException e) {
                LOGGER.warn("Failed to write default leash config to {} — continuing with defaults", cfg.getPath(), e);
            }
            return;
        }

        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(cfg)) {
            p.load(in);
            pullFactorBase = parseDouble(p, "leash.pullFactorBase", pullFactorBase);
            pullFactorPerLoyalty = parseDouble(p, "leash.pullFactorPerLoyalty", pullFactorPerLoyalty);
            verticalFactorUp = parseDouble(p, "leash.verticalFactorUp", verticalFactorUp);
            verticalFactorDown = parseDouble(p, "leash.verticalFactorDown", verticalFactorDown);
            minDistanceFloor = parseDouble(p, "leash.minDistanceFloor", minDistanceFloor);
            minDistanceBase = parseDouble(p, "leash.minDistanceBase", minDistanceBase);
            maxDistanceBase = parseDouble(p, "leash.maxDistanceBase", maxDistanceBase);

            LOGGER.info("Loaded leash config from {}", cfg.getPath());
        } catch (IOException e) {
            LOGGER.warn("Failed to read leash config file {} — using defaults", cfg.getPath(), e);
        }
    }

    private static double parseDouble(Properties p, String key, double def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("Invalid number for {}: {} — using default {}", key, v, def);
            return def;
        }
    }

    public static double getPullFactorBase() {
        return pullFactorBase;
    }

    public static double getPullFactorPerLoyalty() {
        return pullFactorPerLoyalty;
    }

    public static double getVerticalFactorUp() {
        return verticalFactorUp;
    }

    public static double getVerticalFactorDown() {
        return verticalFactorDown;
    }

    public static double getMinDistanceFloor() {
        return minDistanceFloor;
    }

    public static double getMinDistanceBase() {
        return minDistanceBase;
    }

    public static double getMaxDistanceBase() {
        return maxDistanceBase;
    }
}
