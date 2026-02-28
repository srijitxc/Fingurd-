/**
 * FinGuard - Real-Time Fraud Detection System
 * SeverityLevel.java - Enum representing tiered risk severity levels
 */
public enum SeverityLevel {
    SAFE("✅ Safe", 0, 15),
    SUSPICIOUS("⚠️ Suspicious", 16, 30),
    HIGH_RISK("🔴 High Risk", 31, 50),
    FRAUD("🚨 Fraud Alert", 51, Integer.MAX_VALUE);

    private final String label;
    private final int minScore;
    private final int maxScore;

    SeverityLevel(String label, int minScore, int maxScore) {
        this.label = label;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getLabel() { return label; }
    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }

    /**
     * Determine severity tier from a numeric risk score.
     */
    public static SeverityLevel fromScore(int score) {
        if (score <= 15) return SAFE;
        if (score <= 30) return SUSPICIOUS;
        if (score <= 50) return HIGH_RISK;
        return FRAUD;
    }

    @Override
    public String toString() { return label + " (score range: " + minScore + "-" + (maxScore == Integer.MAX_VALUE ? "∞" : maxScore) + ")"; }
}
