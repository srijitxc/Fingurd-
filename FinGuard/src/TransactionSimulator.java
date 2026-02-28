import java.util.*;

/**
 * FinGuard - Real-Time Fraud Detection System
 * TransactionSimulator.java - Generates realistic randomized transactions for demo purposes.
 *
 * Bonus feature: Produces a configurable number of transactions including deliberate
 * fraud scenarios to exercise all 4 detection patterns.
 */
public class TransactionSimulator {
    private static final String[] ACCOUNTS = {
        "ACC501","ACC502","ACC503","ACC504","ACC505",
        "ACC601","ACC602","ACC603","ACC604","ACC605",
        "FRD001","FRD002","FRD003"  // these accounts get targeted fraud injection
    };
    private static final String[] CITIES = {
        "Mumbai","Delhi","Bangalore","Chennai","Hyderabad",
        "Kolkata","Pune","Ahmedabad","Jaipur","Lucknow"
    };
    private static final String[] DEVICES = {
        "DEV10","DEV11","DEV12","DEV13","DEV14",
        "DEV21","DEV22","DEV23","DEV24","DEV25"
    };

    private final int count;
    private final Random random = new Random(42); // seeded for reproducibility

    public TransactionSimulator(int count) {
        this.count = count;
    }

    /**
     * Generate a mixed list of normal + fraudulent transactions.
     * Fraudulent accounts (FRD001, FRD002, FRD003) get crafted scenarios that
     * will trigger all 4 PRD fraud patterns.
     */
    public List<Transaction> generate() {
        List<Transaction> list = new ArrayList<>();
        long baseTime = System.currentTimeMillis() / 1000L - 3600; // 1 hour ago

        // ---- Inject deliberate fraud scenarios for pattern coverage ----

        // Pattern 1: FRD001 — 4 high-value DEBITs > 50,000 in 10 min
        long t1 = baseTime;
        for (int i = 1; i <= 5; i++) {
            list.add(new Transaction(t1 + (i * 90L), "FRD001_HV" + i, "FRD001",
                TransactionType.DEBIT, 55000 + i * 1000, "Mumbai", "DEV10"));
        }

        // Pattern 2: FRD002 — transactions from 3 different cities in 4 minutes
        long t2 = baseTime + 200;
        list.add(new Transaction(t2,       "FRD002_C1", "FRD002", TransactionType.DEBIT, 15000, "Delhi",     "DEV20"));
        list.add(new Transaction(t2 + 60,  "FRD002_C2", "FRD002", TransactionType.DEBIT, 12000, "Mumbai",    "DEV20"));
        list.add(new Transaction(t2 + 180, "FRD002_C3", "FRD002", TransactionType.DEBIT, 18000, "Bangalore", "DEV20"));
        list.add(new Transaction(t2 + 220, "FRD002_C4", "FRD002", TransactionType.DEBIT, 22000, "Chennai",   "DEV20"));

        // Pattern 3: FRD003 — 6 different devices in 10 minutes
        long t3 = baseTime + 400;
        for (int i = 1; i <= 7; i++) {
            list.add(new Transaction(t3 + (i * 70L), "FRD003_D" + i, "FRD003",
                TransactionType.DEBIT, 8000 + i * 500, "Hyderabad", "DEV" + (30 + i)));
        }

        // Pattern 4: FRD001 — spending spike (total > 2,00,000 DEBIT in window)
        long t4 = baseTime + 100;
        for (int i = 1; i <= 4; i++) {
            list.add(new Transaction(t4 + (i * 120L), "FRD001_SP" + i, "FRD001",
                TransactionType.DEBIT, 52000 + i * 2000, "Pune", "DEV10"));
        }

        // ---- Generate remaining normal transactions ----
        int normalCount = Math.max(0, count - list.size());
        for (int i = 0; i < normalCount; i++) {
            String accId  = ACCOUNTS[random.nextInt(ACCOUNTS.length - 3)]; // exclude FRD accounts
            String city   = CITIES[random.nextInt(CITIES.length)];
            String device = DEVICES[random.nextInt(DEVICES.length)];
            TransactionType type = random.nextBoolean() ? TransactionType.DEBIT : TransactionType.CREDIT;
            double amount = 500 + random.nextDouble() * 30000; // normal range 500–30,500

            // Spread transactions over last hour
            long ts = baseTime + (long)(random.nextDouble() * 3600);
            list.add(new Transaction(ts,
                String.format("TXN%06d", 2000 + i),
                accId, type,
                Math.round(amount * 100.0) / 100.0,
                city, device));
        }

        // Shuffle so normal and fraud transactions are interleaved
        Collections.shuffle(list, random);

        System.out.println("[Simulator] Generated " + list.size() + " transactions (" +
                (list.size() - normalCount) + " preset fraud scenarios + " + normalCount + " normal).");
        return list;
    }
}
