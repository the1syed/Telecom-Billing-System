package customer;

import java.sql.*;
import utils.DBConnection;

class AuthService {

    static int authenticate(String phone) {
        try (Connection conn = DBConnection.getConnection()) {
            String fraudCheck = "SELECT 1 FROM FRAUD_NUMBERS WHERE PHONE_NUMBER = ?";
            try (PreparedStatement ps = conn.prepareStatement(fraudCheck)) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("❌ Your number (" + phone + ") has been flagged as spam. Contact support.");
                        return -1;
                    }
                }
            }
            String query = "SELECT CUSTOMER_ID FROM CUSTOMERS WHERE PHONE_NUMBER = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("CUSTOMER_ID");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
        }
        return -1;
    }

    static String getCustomerNameById(int customerId) {
        String sql = "SELECT NAME FROM CUSTOMERS WHERE CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ignored) {}
        return null;
    }
}
