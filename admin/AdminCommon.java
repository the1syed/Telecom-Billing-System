package admin;

public final class AdminCommon {
    private AdminCommon() {}

    public static String nullToEmpty(String s) { return s == null ? "" : s; }

    public static String currentBillingMonth() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%04d%02d", now.getYear(), now.getMonthValue());
    }
}
