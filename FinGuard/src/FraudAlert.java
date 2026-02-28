import java.util.List;
import java.util.ArrayList;

/**
 * FinGuard - Real-Time Fraud Detection System
 * FraudAlert.java - Data model for a generated fraud/risk alert.
 */
public final class FraudAlert {
    /** Sentinel poison-pill alert used to signal shutdown to the dispatcher. */
    public static final FraudAlert POISON_PILL = new FraudAlert("__POISON__", -1, SeverityLevel.SAFE, new ArrayList<>(), 0L);

    private final String accountId;
    private final int riskScore;
    private final SeverityLevel severity;
    private final List<String> reasons;
    private final long detectedAtTimestamp; // epoch seconds of the triggering transaction

    public FraudAlert(String accountId, int riskScore, SeverityLevel severity,
                      List<String> reasons, long detectedAtTimestamp) {
        this.accountId = accountId;
        this.riskScore = riskScore;
        this.severity = severity;
        this.reasons = new ArrayList<>(reasons);
        this.detectedAtTimestamp = detectedAtTimestamp;
    }

    // --- Getters ---
    public String getAccountId()            { return accountId; }
    public int getRiskScore()               { return riskScore; }
    public SeverityLevel getSeverity()      { return severity; }
    public List<String> getReasons()        { return reasons; }
    public long getDetectedAtTimestamp()    { return detectedAtTimestamp; }

    public boolean isPoisonPill() { return "__POISON__".equals(accountId); }

    /** Console-friendly structured alert format as per PRD. */
    public String toAlertString() {
        String tag = severity == SeverityLevel.FRAUD ? "[FRAUD ALERT]" : "[HIGH RISK]";
        return String.format("%s Account: %s | Score: %d | Severity: %s | Reason: %s",
                tag, accountId, riskScore, severity.getLabel(), String.join("; ", reasons));
    }

    /** Full detail string for file output. */
    public String toFileRecord() {
        return String.format("[FRAUD ALERT] Account: %s Score: %d Reason: %s",
                accountId, riskScore, String.join("; ", reasons));
    }

    @Override
    public String toString() { return toAlertString(); }
}
