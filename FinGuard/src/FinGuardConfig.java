import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * FinGuard - Real-Time Fraud Detection System
 * FinGuardConfig.java - Loads configurable detection thresholds from config.properties.
 * Falls back to safe defaults if the file is not found.
 */
public final class FinGuardConfig {
    // --- Detection Thresholds ---
    public final double highValueThreshold;   // INR, per transaction e.g. 50000
    public final double spendingSpikeLimit;   // INR total DEBIT in window e.g. 200000
    public final int windowMinutes;           // main rolling window (fraud patterns 1,3,4)
    public final int cityWindowMinutes;       // location anomaly window (pattern 2)
    public final int maxDeviceSwitches;       // max distinct devices before alert
    public final int maxHighValueTxn;         // max high-value txns before alert
    public final long alertSuppressionSeconds;// suppress duplicate alerts per account

    private FinGuardConfig(double hvt, double ssl, int wm, int cwm, int mds, int mhv, long ass) {
        this.highValueThreshold    = hvt;
        this.spendingSpikeLimit    = ssl;
        this.windowMinutes         = wm;
        this.cityWindowMinutes     = cwm;
        this.maxDeviceSwitches     = mds;
        this.maxHighValueTxn       = mhv;
        this.alertSuppressionSeconds = ass;
    }

    /** Load configuration from the given properties file path. */
    public static FinGuardConfig load(String propertiesPath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            props.load(fis);
            System.out.println("[FinGuard] Loaded configuration from: " + propertiesPath);
        } catch (IOException e) {
            System.out.println("[FinGuard] config.properties not found — using defaults.");
        }

        return new FinGuardConfig(
            getDouble(props, "HIGH_VALUE_THRESHOLD",        50000.0),
            getDouble(props, "SPENDING_SPIKE_LIMIT",       200000.0),
            getInt   (props, "WINDOW_MINUTES",                  10),
            getInt   (props, "CITY_WINDOW_MINUTES",              5),
            getInt   (props, "MAX_DEVICE_SWITCHES",              5),
            getInt   (props, "MAX_HIGH_VALUE_TXN",               3),
            getLong  (props, "ALERT_SUPPRESSION_SECONDS",       60L)
        );
    }

    /** Load with default values. */
    public static FinGuardConfig defaults() {
        return load("resources/config.properties");
    }

    private static double getDouble(Properties p, String key, double def) {
        try { return Double.parseDouble(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    private static int getInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    private static long getLong(Properties p, String key, long def) {
        try { return Long.parseLong(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    @Override
    public String toString() {
        return String.format(
            "[Config] HighValueThreshold=%.0f | SpendingSpike=%.0f | Window=%dmin | CityWindow=%dmin | MaxDevices=%d | MaxHighValue=%d | AlertSuppression=%ds",
            highValueThreshold, spendingSpikeLimit, windowMinutes, cityWindowMinutes,
            maxDeviceSwitches, maxHighValueTxn, alertSuppressionSeconds);
    }
}
