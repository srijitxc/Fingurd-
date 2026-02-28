import java.util.*;
import java.util.concurrent.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * FraudAnalyzer.java - Thread 2: Consumes transactions from the ingestion
 * queue,
 * maintains per-account window state, computes risk scores, and enqueues
 * alerts.
 *
 * Uses ConcurrentHashMap for per-account state with synchronized per-account
 * blocks
 * to avoid race conditions while allowing maximum parallelism across accounts.
 */
public class FraudAnalyzer implements Runnable {
    private final BlockingQueue<Transaction> inputQueue;
    private final BlockingQueue<FraudAlert> alertQueue;
    private final RollingWindowEngine engine;
    private final RiskScorer scorer;
    private final FinGuardConfig config;
    private final StatisticsReporter stats;

    // Per-account window states — ConcurrentHashMap for safe concurrent access
    private final ConcurrentHashMap<String, AccountWindowState> accountStates = new ConcurrentHashMap<>();

    // Track last alert time per account to suppress duplicates
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public FraudAnalyzer(BlockingQueue<Transaction> inputQueue,
            BlockingQueue<FraudAlert> alertQueue,
            RollingWindowEngine engine,
            FinGuardConfig config,
            StatisticsReporter stats) {
        this.inputQueue = inputQueue;
        this.alertQueue = alertQueue;
        this.engine = engine;
        this.scorer = new RiskScorer(engine);
        this.config = config;
        this.stats = stats;
    }

    @Override
    public void run() {
        System.out.println("[Analyzer] Starting — waiting for transactions...");
        int analyzed = 0;

        try {
            // FIX 1: Use poll() with timeout instead of take() so running flag
            // is checked between polls — prevents hanging on forced shutdown.
            while (running) {
                Transaction t = inputQueue.poll(1, TimeUnit.SECONDS);

                if (t == null)
                    continue; // timeout — re-check running

                // Poison pill → ingestion complete; forward signal to dispatcher
                if (isPoison(t)) {
                    forwardPoisonPill();
                    break;
                }

                analyze(t);
                analyzed++;

                if (analyzed % 500 == 0) {
                    System.out.println("[Analyzer] Processed " + analyzed + " transactions so far...");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Analyzer] Interrupted.");
        } finally {
            // FIX 3: Always forward POISON_PILL to dispatcher, even on interrupt
            // or early stop(), so Thread 3 never hangs waiting for it.
            forwardPoisonPill();
            System.out.println("[Analyzer] Done — Analyzed: " + analyzed + " transactions.");
        }
    }

    private void analyze(Transaction t) throws InterruptedException {
        String accId = t.getAccountId();

        // Get or create the per-account window state
        AccountWindowState state = accountStates.computeIfAbsent(accId,
                id -> new AccountWindowState(id, config));

        // FIX 2: Build the alert OUTSIDE the synchronized block.
        // alertQueue.put() can block if the queue is full — calling it while
        // holding the synchronized(state) lock risks a deadlock. Instead, we
        // compute the alert inside the lock (fast, non-blocking), then release
        // the lock before enqueueing it.
        FraudAlert pendingAlert = null;

        synchronized (state) {
            state.addTransaction(t);
            RiskScorer.RiskResult result = scorer.score(state);

            if (result.requiresAlert()) {
                stats.recordRiskScore(accId, result.score);

                if (shouldDispatchAlert(accId, t.getTimestamp())) {
                    pendingAlert = new FraudAlert(
                            accId, result.score, result.severity,
                            result.reasons, t.getTimestamp());
                    lastAlertTime.put(accId, t.getTimestamp());
                    stats.recordAlert(result.severity);
                }
            }
        } // lock released before the potentially-blocking put()

        // Enqueue outside the synchronized block — safe, non-deadlocking
        if (pendingAlert != null) {
            alertQueue.put(pendingAlert);
        }
    }

    /**
     * Forward the POISON_PILL sentinel to the AlertDispatcher so it knows
     * there are no more alerts to dispatch. Idempotent via a flag to prevent
     * sending it multiple times (e.g., once from the pill path, once from finally).
     */
    private volatile boolean poisonSent = false;

    private void forwardPoisonPill() {
        if (poisonSent)
            return;
        poisonSent = true;
        try {
            alertQueue.put(FraudAlert.POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Suppress duplicate alerts for the same account if another alert was
     * dispatched within the suppression window.
     */
    private boolean shouldDispatchAlert(String accId, long nowSeconds) {
        Long last = lastAlertTime.get(accId);
        if (last == null)
            return true;
        return (nowSeconds - last) >= config.alertSuppressionSeconds;
    }

    private boolean isPoison(Transaction t) {
        return "__POISON__".equals(t.getTransactionId());
    }

    public void stop() {
        running = false;
    }

    /** Returns a snapshot map of all account states (for reporting). */
    public Map<String, AccountWindowState> getAccountStates() {
        return Collections.unmodifiableMap(accountStates);
    }
}
