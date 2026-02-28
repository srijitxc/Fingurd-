import java.util.ArrayList;
import java.util.List;

/**
 * FinGuard - Real-Time Fraud Detection System
 * RiskScorer.java - Computes a numeric risk score from AccountWindowState.
 *
 * Formula (per PRD):
 *   Risk Score = (highValueTransactions × 5) + (cityChanges × 7)
 *              + (deviceSwitches × 4) + totalSpendingFactor
 *
 * Where:
 *   - cityChanges    = distinctCities - 1 (number of additional cities beyond the first)
 *   - deviceSwitches = uniqueDevices - 1  (switches beyond the first)
 *   - spendingFactor = totalDebitSpend / 10000, capped at 30
 */
public class RiskScorer {
    private final RollingWindowEngine engine;
    private final FinGuardConfig config;

    public RiskScorer(RollingWindowEngine engine) {
        this.engine = engine;
        this.config = engine.getConfig();
    }

    /**
     * Compute risk score for the given account window state.
     * Also evaluates all rules and bundles results into a RiskResult.
     */
    public RiskResult score(AccountWindowState state) {
        List<FraudDetectionRule> violatedRules = engine.evaluateRules(state);

        // Compute score components
        int highValueScore   = state.getHighValueCount() * 5;
        int cityChangeScore  = Math.max(0, state.getDistinctCitiesInCityWindow() - 1) * 7;
        int deviceScore      = Math.max(0, state.getUniqueDeviceCount() - 1) * 4;
        int spendScore       = (int) Math.min(state.getTotalDebitSpend() / 10000, 30);

        // Guard: If no activity, score is zero
        if (!state.hasActivity()) {
            return new RiskResult(0, SeverityLevel.SAFE, new ArrayList<>(), violatedRules);
        }

        int totalScore = highValueScore + cityChangeScore + deviceScore + spendScore;

        SeverityLevel severity = SeverityLevel.fromScore(totalScore);

        // Collect human-readable reasons from violated rules
        List<String> reasons = new ArrayList<>();
        for (FraudDetectionRule rule : violatedRules) {
            reasons.add(rule.getReason(state, config));
        }

        return new RiskResult(totalScore, severity, reasons, violatedRules);
    }

    /** Immutable result object returned by score(). */
    public static final class RiskResult {
        public final int score;
        public final SeverityLevel severity;
        public final List<String> reasons;
        public final List<FraudDetectionRule> violatedRules;

        public RiskResult(int score, SeverityLevel severity,
                          List<String> reasons, List<FraudDetectionRule> violatedRules) {
            this.score        = score;
            this.severity     = severity;
            this.reasons      = new ArrayList<>(reasons);
            this.violatedRules = new ArrayList<>(violatedRules);
        }

        public boolean requiresAlert() {
            return severity == SeverityLevel.HIGH_RISK || severity == SeverityLevel.FRAUD;
        }
    }
}
