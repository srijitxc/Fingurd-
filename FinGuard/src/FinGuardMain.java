import java.util.*;
import java.util.concurrent.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * FinGuardMain.java - Entry point. Wires the 3-thread pipeline and manages lifecycle.
 *
 * Usage:
 *   java FinGuardMain --file <path/to/transactions.csv>
 *   java FinGuardMain --simulate <count>
 *   java FinGuardMain                    (defaults to --simulate 2000)
 */
public class FinGuardMain {
    private static final int QUEUE_CAPACITY = 1000;

    public static void main(String[] args) throws InterruptedException {
        printBanner();

        // --- 1. Load Configuration ---
        FinGuardConfig config = FinGuardConfig.defaults();
        System.out.println(config);
        System.out.println();

        // --- 2. Parse CLI arguments ---
        String mode     = "simulate";
        String filePath = "resources/transactions.csv";
        int    simCount = 2000;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file":
                    mode     = "file";
                    filePath = (i + 1 < args.length) ? args[++i] : filePath;
                    break;
                case "--simulate":
                    mode     = "simulate";
                    simCount = (i + 1 < args.length) ? parseInt(args[++i], 2000) : 2000;
                    break;
                default:
                    System.err.println("[Main] Unknown argument: " + args[i]);
            }
        }

        System.out.println("[Main] Mode: " + mode.toUpperCase() + (mode.equals("file") ? " | File: " + filePath : " | Count: " + simCount));

        // --- 3. Shared queues ---
        BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<FraudAlert>  alertQueue       = new LinkedBlockingQueue<>(500);

        // --- 4. Statistics reporter ---
        StatisticsReporter stats = new StatisticsReporter();

        // --- 5. Build pipeline components ---
        RollingWindowEngine engine     = new RollingWindowEngine(config);
        TransactionIngester ingester   = new TransactionIngester(filePath, transactionQueue, stats);
        FraudAnalyzer       analyzer   = new FraudAnalyzer(transactionQueue, alertQueue, engine, config, stats);
        AlertDispatcher     dispatcher = new AlertDispatcher(alertQueue, stats);

        // --- 6. Register shutdown hook for graceful termination + stats ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] Shutdown hook triggered.");
            ingester.stop();
            analyzer.stop();
            dispatcher.stop();
            stats.printSummary();
        }, "FinGuard-ShutdownHook"));

        // --- 7. Start the 3-thread pipeline via ExecutorService ---
        ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setDaemon(false); // keep JVM alive until all threads complete
            return t;
        });

        long startMs = System.currentTimeMillis();

        if ("file".equals(mode)) {
            // Thread 1 reads from file
            executor.submit(ingester);
        } else {
            // Snapshot simCount as effectively-final for lambda capture
            final int finalSimCount = simCount;
            // Thread 1 runs simulation inline then enqueues
            executor.submit(() -> {
                TransactionSimulator simulator = new TransactionSimulator(finalSimCount);
                List<Transaction> transactions = simulator.generate();
                ingester.ingestFromList(transactions);
            });
        }

        // Thread 2: FraudAnalyzer
        executor.submit(analyzer);

        // Thread 3: AlertDispatcher
        executor.submit(dispatcher);

        // --- 8. Graceful shutdown — wait for all threads to finish ---
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.MINUTES);

        long elapsed = System.currentTimeMillis() - startMs;

        if (finished) {
            System.out.printf("%n[Main] All threads completed in %d ms.%n", elapsed);
        } else {
            System.err.println("[Main] WARN — Timeout waiting for threads. Forcing shutdown.");
            executor.shutdownNow();
        }

        stats.printSummary();
        System.out.println("[Main] FinGuard session ended. Check fraud_alert.txt for fraud records.");
    }

    private static int parseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   FinGuard — Real-Time Fraud Detection System         ║");
        System.out.println("║   IBM Hackathon 2026  |  Java  |  In-Memory Engine    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
