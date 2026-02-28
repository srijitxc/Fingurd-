import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * StatisticsReporter.java - Tracks runtime statistics and prints a summary report on shutdown.
 *
 * Bonus feature: provides total transactions, alert counts, and top 5 risky accounts.
 * Thread-safe using AtomicInteger and ConcurrentHashMap.
 */
public class StatisticsReporter {
    private final AtomicLong totalIngested  = new AtomicLong(0);
    private final AtomicLong totalSafe      = new AtomicLong(0);
    private final AtomicLong totalSuspicious= new AtomicLong(0);
    private final AtomicLong totalHighRisk  = new AtomicLong(0);
    private final AtomicLong totalFraud     = new AtomicLong(0);

    // Per-account peak risk score
    private final ConcurrentHashMap<String, Integer> accountPeakScores = new ConcurrentHashMap<>();

    private final long startTimeMs = System.currentTimeMillis();

    /** Called by Ingester for each accepted transaction. */
    public void recordIngested() { totalIngested.incrementAndGet(); }

    /** Called by Analyzer when an alert-worthy event passes the scorer. */
    public void recordRiskScore(String accountId, int score) {
        accountPeakScores.merge(accountId, score, Math::max);
    }

    /** Called by Analyzer when an alert is dispatched. */
    public void recordAlert(SeverityLevel level) {
        switch (level) {
            case SAFE:       totalSafe.incrementAndGet();       break;
            case SUSPICIOUS: totalSuspicious.incrementAndGet(); break;
            case HIGH_RISK:  totalHighRisk.incrementAndGet();   break;
            case FRAUD:      totalFraud.incrementAndGet();      break;
        }
    }

    /** Prints a summary report to stdout. Called at system shutdown. */
    public void printSummary() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        long totalAlerts = totalHighRisk.get() + totalFraud.get();

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║         FINGUA RD — SESSION SUMMARY REPORT           ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf( "║  Total Transactions Processed : %-20d ║%n", totalIngested.get());
        System.out.printf( "║  Elapsed Time                 : %-18s ms ║%n", elapsed);
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf( "║  🔴 High Risk Alerts          : %-20d ║%n", totalHighRisk.get());
        System.out.printf( "║  🚨 Fraud Alerts              : %-20d ║%n", totalFraud.get());
        System.out.printf( "║  Total Alerts Raised          : %-20d ║%n", totalAlerts);
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Top 5 Highest Risk Accounts:                        ║");

        // Sort accounts by peak score descending, take top 5
        accountPeakScores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> System.out.printf("║    %-20s  Peak Score: %-10d  ║%n", e.getKey(), e.getValue()));

        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
