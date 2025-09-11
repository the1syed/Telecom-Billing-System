package customer;

import java.sql.*;
import utils.DBConnection;
import utils.InputUtil;

class FraudService {

    static void reportSpam(int customerId) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        String reportedNumber;
        try { reportedNumber = InputUtil.readPhoneNumber(sc, "Enter the phone number you want to report as spam: "); }
        catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts. Cancelling spam report."); return; }

        String insertReport = "INSERT INTO SPAM_REPORTS (REPORT_ID, CUSTOMER_ID, REPORTED_NUMBER) VALUES (REPORT_SEQ.NEXTVAL, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(insertReport)) {
            ps.setInt(1, customerId); ps.setString(2, reportedNumber); ps.executeUpdate();
            System.out.println("✅ Spam report submitted successfully.");
            checkAndMarkFraud(conn, reportedNumber);
        } catch (SQLException e) { System.out.println("Error while reporting spam: " + e.getMessage()); }
    }

    private static void checkAndMarkFraud(Connection conn, String reportedNumber) throws SQLException {
        String totalCustomersQuery = "SELECT COUNT(*) AS CNT FROM CUSTOMERS";
        String reportCountQuery = "SELECT COUNT(DISTINCT CUSTOMER_ID) AS CNT FROM SPAM_REPORTS WHERE REPORTED_NUMBER = ?";
        int totalCustomers = 0, reportCount = 0;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(totalCustomersQuery)) {
            if (rs.next()) totalCustomers = rs.getInt("CNT");
        }
        try (PreparedStatement ps = conn.prepareStatement(reportCountQuery)) {
            ps.setString(1, reportedNumber);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) reportCount = rs.getInt("CNT"); }
        }
        if (totalCustomers > 0 && (reportCount * 100.0 / totalCustomers) >= 40) {
            String insertFraud = "MERGE INTO FRAUD_NUMBERS f USING (SELECT ? AS PHONE_NUMBER FROM dual) src ON (f.PHONE_NUMBER = src.PHONE_NUMBER) WHEN NOT MATCHED THEN INSERT (ID, PHONE_NUMBER, REASON) VALUES (FRAUD_SEQ.NEXTVAL, src.PHONE_NUMBER, 'Reported by users')";
            try (PreparedStatement ps = conn.prepareStatement(insertFraud)) { ps.setString(1, reportedNumber); ps.executeUpdate(); System.out.println("🚨 Number " + reportedNumber + " has been flagged as FRAUD (40% rule)."); }
        }
    }
}
