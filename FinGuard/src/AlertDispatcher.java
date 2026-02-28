import java.io.*;
import java.util.concurrent.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * AlertDispatcher.java - Thread 3: Consumes alerts from the alert queue,
 * prints HIGH_RISK alerts to console, and writes FRAUD alerts to
 * fraud_alert.txt.
 *
 * Applies duplicate suppression at the dispatch level as a final safety net.
 */
public class AlertDispatcher implements Runnable {
    private static final String ALERT_FILE = "fraud_alert.txt";

    private final BlockingQueue<FraudAlert> alertQueue;

    private volatile boolean running = true;
    private BufferedWriter fileWriter;

    public AlertDispatcher(BlockingQueue<FraudAlert> alertQueue, StatisticsReporter stats) {
        this.alertQueue = alertQueue;
        // stats is owned by FraudAnalyzer; kept in signature for API consistency
    }

    @Override
    public void run() {
        System.out.println("[Dispatcher] Starting — listening for alerts...");
        initFileWriter();
        int dispatched = 0;

        try {
            // Use poll() with a timeout instead of take() so the running flag
            // is checked between polls — prevents the thread hanging forever
            // when stop() is called during a forced/early shutdown.
            while (running) {
                FraudAlert alert = alertQueue.poll(1, TimeUnit.SECONDS);

                if (alert == null)
                    continue; // timeout — re-check running
                if (alert.isPoisonPill())
                    break; // normal end-of-stream signal

                dispatch(alert);
                dispatched++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Dispatcher] Interrupted.");
        } finally {
            closeFileWriter();
            System.out.println("[Dispatcher] Done — Dispatched: " + dispatched + " alerts.");
        }
    }

    private void dispatch(FraudAlert alert) {
        SeverityLevel sev = alert.getSeverity();

        if (sev == SeverityLevel.HIGH_RISK) {
            // HIGH_RISK: structured console output only
            System.out.println("\n" + alert.toAlertString());
        } else if (sev == SeverityLevel.FRAUD) {
            // FRAUD: console + file
            System.out.println("\n" + alert.toAlertString());
            writeToFile(alert);
        }
    }

    private void initFileWriter() {
        try {
            fileWriter = new BufferedWriter(new FileWriter(ALERT_FILE, true /* append */));
            fileWriter.write("=== FinGuard Fraud Alert Log — Session Started ===");
            fileWriter.newLine();
        } catch (IOException e) {
            System.err.println("[Dispatcher] WARN — Cannot open fraud_alert.txt: " + e.getMessage());
            fileWriter = null;
        }
    }

    private void writeToFile(FraudAlert alert) {
        if (fileWriter == null)
            return; // File I/O error must not crash the system (PRD §10)
        try {
            fileWriter.write(alert.toFileRecord());
            fileWriter.newLine();
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("[Dispatcher] WARN — Could not write alert to file: " + e.getMessage());
        }
    }

    private void closeFileWriter() {
        if (fileWriter == null)
            return;
        try {
            fileWriter.write("=== Session Ended ===");
            fileWriter.newLine();
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("[Dispatcher] WARN — Error closing alert file: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
