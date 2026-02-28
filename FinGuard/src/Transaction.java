/**
 * FinGuard - Real-Time Fraud Detection System
 * Transaction.java - Core data model representing a single financial transaction.
 *
 * Format: timestamp, transactionId, accountId, transactionType, amount, location, deviceId
 * Example: 1708069200, TXN1001, ACC501, DEBIT, 25000, Mumbai, DEV88
 */
public final class Transaction {
    private final long timestamp;          // Unix epoch seconds
    private final String transactionId;   // Unique identifier e.g. TXN1001
    private final String accountId;       // Account e.g. ACC501
    private final TransactionType transactionType; // DEBIT or CREDIT
    private final double amount;          // Value in INR
    private final String location;        // City of origin
    private final String deviceId;        // Device used e.g. DEV88

    public Transaction(long timestamp, String transactionId, String accountId,
                       TransactionType transactionType, double amount,
                       String location, String deviceId) {
        this.timestamp = timestamp;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.location = location;
        this.deviceId = deviceId;
    }

    // --- Getters ---
    public long getTimestamp()                 { return timestamp; }
    public String getTransactionId()           { return transactionId; }
    public String getAccountId()               { return accountId; }
    public TransactionType getTransactionType(){ return transactionType; }
    public double getAmount()                  { return amount; }
    public String getLocation()                { return location; }
    public String getDeviceId()                { return deviceId; }

    /**
     * Parse a CSV line into a Transaction object.
     * Expected format: timestamp, transactionId, accountId, type, amount, location, deviceId
     */
    public static Transaction parse(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length < 7) {
            throw new IllegalArgumentException("Malformed transaction line: " + csvLine);
        }
        long ts = Long.parseLong(parts[0].trim());
        String txnId = parts[1].trim();
        String accId = parts[2].trim();
        TransactionType type = TransactionType.fromString(parts[3].trim());
        double amt = Double.parseDouble(parts[4].trim());
        String loc = parts[5].trim();
        String dev = parts[6].trim();
        return new Transaction(ts, txnId, accId, type, amt, loc, dev);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, acc=%s, type=%s, amount=%.2f, location=%s, device=%s, ts=%d}",
                transactionId, accountId, transactionType, amount, location, deviceId, timestamp);
    }
}
