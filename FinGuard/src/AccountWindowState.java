import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * FinGuard - Real-Time Fraud Detection System
 * AccountWindowState.java - Maintains a per-account rolling time-window state.
 *
 * Uses a Deque to store transactions in arrival order. Old transactions are
 * evicted lazily when a new transaction arrives, keeping all operations O(1) amortised.
 *
 * Maintains incremental counters so the RollingWindowEngine never needs O(n) rescans.
 */
public class AccountWindowState {
    private final String accountId;
    private final long windowSeconds;      // main window in seconds (default 600 = 10 min)
    private final long cityWindowSeconds;  // city anomaly window in seconds (default 300 = 5 min)

    // --- Rolling window deque (main window) ---
    private final Deque<Transaction> mainWindow  = new ArrayDeque<>();

    // --- Incremental counters (updated on every add/evict) ---
    private int    highValueCount  = 0;    // DEBIT transactions above HIGH_VALUE_THRESHOLD
    private double totalDebitSpend = 0.0;  // sum of all DEBIT amounts in window
    private final Set<String> deviceIds = new HashSet<>();  // distinct device IDs seen
    private int    totalTransactions = 0;  // count of all transactions in main window

    // --- City anomaly window (separate, shorter deque) ---
    private final Deque<Transaction> cityWindow = new ArrayDeque<>();
    private final List<String>       recentCities = new LinkedList<>(); // ordered city history

    // --- Thresholds loaded from config ---
    private final double highValueThreshold;

    public AccountWindowState(String accountId, FinGuardConfig config) {
        this.accountId         = accountId;
        this.windowSeconds     = config.windowMinutes * 60L;
        this.cityWindowSeconds = config.cityWindowMinutes * 60L;
        this.highValueThreshold = config.highValueThreshold;
    }

    /**
     * Add a new transaction to this account's rolling windows.
     * Evicts expired transactions first, then updates all counters.
     * Thread-safety is handled by the caller (RollingWindowEngine uses synchronized blocks).
     */
    public void addTransaction(Transaction t) {
        long cutoffMain = t.getTimestamp() - windowSeconds;
        long cutoffCity = t.getTimestamp() - cityWindowSeconds;

        // Evict stale entries from main window
        evictMainWindow(cutoffMain);

        // Evict stale entries from city window
        evictCityWindow(cutoffCity);

        // --- Add to main window ---
        mainWindow.addLast(t);
        totalTransactions++;

        if (t.getTransactionType() == TransactionType.DEBIT) {
            totalDebitSpend += t.getAmount();
            if (t.getAmount() > highValueThreshold) {
                highValueCount++;
            }
        }
        deviceIds.add(t.getDeviceId());

        // --- Add to city window ---
        cityWindow.addLast(t);
        recentCities.add(t.getLocation());
    }

    /** Evict main-window entries older than cutoff epoch. */
    private void evictMainWindow(long cutoff) {
        while (!mainWindow.isEmpty() && mainWindow.peekFirst().getTimestamp() < cutoff) {
            Transaction old = mainWindow.pollFirst();
            totalTransactions--;
            if (old.getTransactionType() == TransactionType.DEBIT) {
                totalDebitSpend -= old.getAmount();
                if (old.getAmount() > highValueThreshold) {
                    highValueCount--;
                }
            }
            // NOTE: deviceIds set is NOT decremented on eviction (a device might still be in use from a newer tx).
            // We recompute deviceIds from scratch during eviction when needed — see getUniqueDeviceCount().
        }
        // Recompute deviceIds from current main window (ensures accuracy after eviction)
        deviceIds.clear();
        for (Transaction t : mainWindow) {
            deviceIds.add(t.getDeviceId());
        }
    }

    /** Evict city-window entries older than cutoff epoch. */
    private void evictCityWindow(long cutoff) {
        while (!cityWindow.isEmpty() && cityWindow.peekFirst().getTimestamp() < cutoff) {
            cityWindow.pollFirst();
            if (!recentCities.isEmpty()) {
                recentCities.remove(0);
            }
        }
    }

    // --- Accessors for RollingWindowEngine / RiskScorer ---

    public String getAccountId()            { return accountId; }
    public int getHighValueCount()          { return Math.max(0, highValueCount); }
    public double getTotalDebitSpend()      { return Math.max(0.0, totalDebitSpend); }
    public int getUniqueDeviceCount()       { return deviceIds.size(); }
    public int getTotalTransactionsInWindow(){ return totalTransactions; }

    /** Number of distinct cities seen in the short city window. */
    public int getDistinctCitiesInCityWindow() {
        Set<String> distinctCities = new HashSet<>(recentCities);
        return distinctCities.size();
    }

    /** Whether there is any transaction data in the main window. */
    public boolean hasActivity() { return !mainWindow.isEmpty(); }

    @Override
    public String toString() {
        return String.format("AccountWindow{acc=%s, txns=%d, highValue=%d, totalDebit=%.0f, devices=%d, cities=%d}",
                accountId, totalTransactions, highValueCount, totalDebitSpend,
                deviceIds.size(), getDistinctCitiesInCityWindow());
    }
}
