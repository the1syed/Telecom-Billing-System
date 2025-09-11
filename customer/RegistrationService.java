package customer;

import java.sql.*;
import java.util.concurrent.ThreadLocalRandom;
import utils.DBConnection;

class RegistrationService {

    static class RegistrationResult {
        final int customerId;
        final String phoneNumber;
        RegistrationResult(int id, String phone) { this.customerId = id; this.phoneNumber = phone; }
    }

    static int register(String name, String phone, String address) {
        String sql = "INSERT INTO CUSTOMERS (NAME, PHONE_NUMBER, ADDRESS) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "CUSTOMER_ID" })) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, address);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) { System.out.println("Registration error: " + e.getMessage()); }
        return -1;
    }

    static RegistrationResult registerAuto(String name, String address) {
        return registerAutoWithPlan(name, address, null, "POSTPAID");
    }

    static RegistrationResult registerAutoWithPlan(String name, String address, Integer planId, String accountType) {
        if (accountType == null || (!accountType.equals("PREPAID") && !accountType.equals("POSTPAID"))) accountType = "POSTPAID";
        String base = (planId == null)
                ? "INSERT INTO CUSTOMERS (NAME, PHONE_NUMBER, ADDRESS, ACCOUNT_TYPE) VALUES (?, ?, ?, ?)"
                : "INSERT INTO CUSTOMERS (NAME, PHONE_NUMBER, ADDRESS, ACCOUNT_TYPE, PLAN_ID) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            String phone = generateUniquePhone(conn);
            try (PreparedStatement ps = conn.prepareStatement(base, new String[] { "CUSTOMER_ID" })) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, address);
                ps.setString(4, accountType);
                if (planId != null) ps.setInt(5, planId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) { int id = rs.getInt(1); return new RegistrationResult(id, phone); }
                    }
                }
            }
        } catch (SQLException e) { System.out.println("Registration error: " + e.getMessage()); }
        return null;
    }

    private static String generateUniquePhone(Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM CUSTOMERS WHERE PHONE_NUMBER = ? FETCH FIRST 1 ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            while (true) {
                String candidate = "9" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
                ps.setString(1, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return candidate;
                }
            }
        }
    }
}
