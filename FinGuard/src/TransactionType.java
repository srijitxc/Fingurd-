/**
 * FinGuard - Real-Time Fraud Detection System
 * TransactionType.java - Enum representing transaction direction
 */
public enum TransactionType {
    DEBIT,
    CREDIT;

    public static TransactionType fromString(String s) {
        switch (s.trim().toUpperCase()) {
            case "DEBIT": return DEBIT;
            case "CREDIT": return CREDIT;
            default: throw new IllegalArgumentException("Unknown transaction type: " + s);
        }
    }
}
