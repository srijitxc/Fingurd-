import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FinGuard - Real-Time Fraud Detection System
 * RollingWindowEngine.java - Evaluates fraud detection rules against account window states.
 *
 * Implements all 4 fraud detection patterns from the PRD using the FraudDetectionRule interface.
 * Uses a ConcurrentHashMap of AccountWindowState objects keyed by accountId.
 */
public class RollingWindowEngine {
    private final FinGuardConfig config;
    private final List<FraudDetectionRule> rules;

    // All registered fraud detection rules (Pattern 1-4 from PRD)
    public RollingWindowEngine(FinGuardConfig config) {
        this.config = config;
        this.rules  = buildRules();
    }

    /** Build all 4 PRD fraud detection rules. */
    private List<FraudDetectionRule> buildRules() {
        return Arrays.asList(
            // Pattern 1: High-value transactions (>3 above ₹50,000 in 10 min from same account)
            new FraudDetectionRule() {
                public String getName() { return "HighValueTransactions"; }
                public boolean isViolated(AccountWindowState s, FinGuardConfig c) {
                    return s.getHighValueCount() > c.maxHighValueTxn;
                }
                public String getReason(AccountWindowState s, FinGuardConfig c) {
                    return String.format("High-value: %d transactions above ₹%.0f in 10min",
                            s.getHighValueCount(), c.highValueThreshold);
                }
                public int getScoreContribution(AccountWindowState s, FinGuardConfig c) {
                    return s.getHighValueCount() * 5;
                }
            },

            // Pattern 2: Location anomaly (2+ cities in 5-min window)
            new FraudDetectionRule() {
                public String getName() { return "LocationAnomaly"; }
                public boolean isViolated(AccountWindowState s, FinGuardConfig c) {
                    return s.getDistinctCitiesInCityWindow() >= 2;
                }
                public String getReason(AccountWindowState s, FinGuardConfig c) {
                    return String.format("Location anomaly: transactions from %d different cities in %dmin",
                            s.getDistinctCitiesInCityWindow(), c.cityWindowMinutes);
                }
                public int getScoreContribution(AccountWindowState s, FinGuardConfig c) {
                    int cityChanges = Math.max(0, s.getDistinctCitiesInCityWindow() - 1);
                    return cityChanges * 7;
                }
            },

            // Pattern 3: Device switching (>5 different devices in 10 min)
            new FraudDetectionRule() {
                public String getName() { return "DeviceSwitching"; }
                public boolean isViolated(AccountWindowState s, FinGuardConfig c) {
                    return s.getUniqueDeviceCount() > c.maxDeviceSwitches;
                }
                public String getReason(AccountWindowState s, FinGuardConfig c) {
                    return String.format("Device switching: %d unique devices used in 10min",
                            s.getUniqueDeviceCount());
                }
                public int getScoreContribution(AccountWindowState s, FinGuardConfig c) {
                    int switches = Math.max(0, s.getUniqueDeviceCount() - 1);
                    return switches * 4;
                }
            },

            // Pattern 4: Spending spike (total DEBIT > ₹2,00,000 in 10 min)
            new FraudDetectionRule() {
                public String getName() { return "SpendingSpike"; }
                public boolean isViolated(AccountWindowState s, FinGuardConfig c) {
                    return s.getTotalDebitSpend() > c.spendingSpikeLimit;
                }
                public String getReason(AccountWindowState s, FinGuardConfig c) {
                    return String.format("Spending spike: ₹%.0f total DEBIT in 10min (limit ₹%.0f)",
                            s.getTotalDebitSpend(), c.spendingSpikeLimit);
                }
                public int getScoreContribution(AccountWindowState s, FinGuardConfig c) {
                    int factor = (int)(s.getTotalDebitSpend() / 10000);
                    return Math.min(factor, 30); // capped to prevent overflow
                }
            }
        );
    }

    /**
     * Evaluate all fraud detection rules against the given account state.
     * Returns a list of violated rules (may be empty).
     */
    public List<FraudDetectionRule> evaluateRules(AccountWindowState state) {
        List<FraudDetectionRule> violated = new ArrayList<>();
        for (FraudDetectionRule rule : rules) {
            if (rule.isViolated(state, config)) {
                violated.add(rule);
            }
        }
        return violated;
    }

    public FinGuardConfig getConfig() { return config; }
}
