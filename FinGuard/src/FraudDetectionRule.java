/**
 * FinGuard - Real-Time Fraud Detection System
 * FraudDetectionRule.java - Interface for pluggable, single-responsibility fraud rules.
 *
 * Each rule encapsulates one specific fraud pattern. New patterns can be added
 * without touching the engine — just implement this interface.
 */
public interface FraudDetectionRule {
    /** Human-readable rule name. */
    String getName();

    /**
     * Evaluate whether this rule is violated for the given account window state.
     * @param state current rolling-window state of the account
     * @param config system configuration (thresholds)
     * @return true if the rule is triggered (fraud pattern detected)
     */
    boolean isViolated(AccountWindowState state, FinGuardConfig config);

    /**
     * Human-readable reason string to include in the alert.
     * Only called when isViolated() returns true.
     */
    String getReason(AccountWindowState state, FinGuardConfig config);

    /**
     * Score contribution when this rule is violated.
     * Implementations should return a consistent value.
     */
    int getScoreContribution(AccountWindowState state, FinGuardConfig config);
}
