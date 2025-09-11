package customer;

import java.sql.*;
import java.util.Scanner;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AccountService {

    static void viewMyDetails(int customerId) {
        Scanner sc = new Scanner(System.in);
        String query = "SELECT c.NAME, c.PHONE_NUMBER, c.ADDRESS, p.PLAN_NAME FROM CUSTOMERS c LEFT JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID WHERE c.CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.util.List<String> headers = java.util.List.of("Name", "Phone", "Address", "Plan");
                    java.util.List<java.util.List<String>> rows = java.util.List.of(java.util.List.of(
                            rs.getString("NAME"),
                            rs.getString("PHONE_NUMBER"),
                            nullToEmpty(rs.getString("ADDRESS")),
                            rs.getString("PLAN_NAME") == null ? "(none)" : rs.getString("PLAN_NAME")));
                    System.out.println();
                    System.out.print(DataViewFormatter.table(headers, rows));
                } else {
                    System.out.println("Customer not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving details: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void updateProfile(int customerId, Scanner sc) {
        System.out.println("\n--- Update Profile (Name & Address only) ---");
        String newName = InputUtil.readNonEmptyLine(sc, "Enter new name (leave blank to keep current): ", true);
        String newAddress = InputUtil.readNonEmptyLine(sc, "Enter new address (leave blank to keep current): ", true);

        if (newName.isEmpty() && newAddress.isEmpty()) {
            System.out.println("Nothing changed.");
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE CUSTOMERS SET ");
        boolean first = true;
        if (!newName.isEmpty()) {
            sql.append("NAME = ?");
            first = false;
        }
        if (!newAddress.isEmpty()) {
            if (!first)
                sql.append(", ");
            sql.append("ADDRESS = ?");
        }
        sql.append(" WHERE CUSTOMER_ID = ?");

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!newName.isEmpty())
                ps.setString(idx++, newName);
            if (!newAddress.isEmpty())
                ps.setString(idx++, newAddress);
            ps.setInt(idx, customerId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("Profile updated.");
            else
                System.out.println("Update failed.");
        } catch (SQLException e) {
            System.out.println("Error updating profile: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void deleteProfile(int customerId) {
        String sql = "DELETE FROM CUSTOMERS WHERE CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("Profile deleted.");
            else
                System.out.println("Profile not found.");
        } catch (SQLException e) {
            System.out.println("Error deleting profile: " + e.getMessage());
        }
    }

    static String getCustomerNameById(int customerId) {
        String sql = "SELECT NAME FROM CUSTOMERS WHERE CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString(1);
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    static String getAccountType(int customerId) {
        String sql = "SELECT ACCOUNT_TYPE FROM CUSTOMERS WHERE CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString(1);
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
