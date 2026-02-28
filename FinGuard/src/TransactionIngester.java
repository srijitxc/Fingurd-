import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * TransactionIngester.java - Thread 1: Reads transactions from a CSV file,
 * parses them into Transaction objects and enqueues them for analysis.
 *
 * Sends a POISON_PILL Transaction to signal end-of-stream to downstream threads.
 */
public class TransactionIngester implements Runnable {
    /** Sentinel poison-pill to signal end-of-stream. */
    public static final Transaction POISON_PILL =
        new Transaction(-1L, "__POISON__", "__POISON__", TransactionType.CREDIT, 0d, "NONE", "NONE");

    private final String filePath;
    private final BlockingQueue<Transaction> outputQueue;
    private final StatisticsReporter stats;

    // Deduplication: tracks seen transaction IDs
    private final Set<String> seenIds = new HashSet<>();

    private volatile boolean running = true;

    public TransactionIngester(String filePath,
                               BlockingQueue<Transaction> outputQueue,
                               StatisticsReporter stats) {
        this.filePath    = filePath;
        this.outputQueue = outputQueue;
        this.stats       = stats;
    }

    @Override
    public void run() {
        System.out.println("[Ingester] Starting — reading from: " + filePath);
        int accepted = 0, skipped = 0, duplicates = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();

                // Skip blank lines and comment/header lines
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("timestamp")) {
                    continue;
                }

                try {
                    Transaction t = Transaction.parse(line);

                    // Deduplicate by transactionId
                    if (seenIds.contains(t.getTransactionId())) {
                        duplicates++;
                        continue;
                    }
                    seenIds.add(t.getTransactionId());

                    outputQueue.put(t); // blocks if queue is full (back-pressure)
                    accepted++;
                    stats.recordIngested();

                } catch (IllegalArgumentException e) {
                    System.err.println("[Ingester] WARN — Skipping malformed line: " + line + " | " + e.getMessage());
                    skipped++;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("[Ingester] ERROR — File not found: " + filePath);
        } catch (IOException e) {
            System.err.println("[Ingester] ERROR — IO error reading file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Ingester] Interrupted during enqueue.");
        } finally {
            // Send poison pill to signal downstream that ingestion is done
            try {
                outputQueue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.printf("[Ingester] Done — Accepted: %d | Skipped: %d | Duplicates: %d%n",
                    accepted, skipped, duplicates);
        }
    }

    /** Also enqueues transactions from an in-memory list (used by simulator). */
    public void ingestFromList(List<Transaction> transactions) {
        System.out.println("[Ingester] Ingesting " + transactions.size() + " simulated transactions.");
        int accepted = 0;
        for (Transaction t : transactions) {
            if (!running) break;
            try {
                if (!seenIds.contains(t.getTransactionId())) {
                    seenIds.add(t.getTransactionId());
                    outputQueue.put(t);
                    accepted++;
                    stats.recordIngested();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        try {
            outputQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[Ingester] Simulation done — Accepted: " + accepted);
    }

    public void stop() { running = false; }
}
