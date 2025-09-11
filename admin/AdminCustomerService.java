package admin;

import java.sql.*;
import java.util.Scanner;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AdminCustomerService {

    static void viewAllCustomers() {
        Scanner sc = new Scanner(System.in);
        String sql = "SELECT c.NAME, c.PHONE_NUMBER, c.ADDRESS, c.ACCOUNT_TYPE, p.PLAN_NAME, p.PLAN_TYPE FROM CUSTOMERS c LEFT JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID ORDER BY c.NAME";
        try (Connection con = DBConnection.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(java.util.List.of(
                        rs.getString("NAME"),
                        rs.getString("PHONE_NUMBER"),
                        AdminCommon.nullToEmpty(rs.getString("ADDRESS")),
                        rs.getString("ACCOUNT_TYPE"),
                        rs.getString("PLAN_NAME") == null ? "(none)" : rs.getString("PLAN_NAME"),
                        rs.getString("PLAN_TYPE") == null ? "-" : rs.getString("PLAN_TYPE")
                ));
            }
            System.out.println();
            System.out.println("CUSTOMERS");
            System.out.print(DataViewFormatter.table(java.util.List.of("Name", "Phone", "Address", "AcctType", "Plan", "PlanType"), rows));
        } catch (SQLException e) { System.out.println("Error loading customers: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void viewCustomerByPhone(Scanner sc) {
        String phone;
        try {
            phone = InputUtil.readPhoneNumberOrCancel(sc, "Enter Customer Phone Number (10 digits, or 0 to cancel): ");
        } catch (InputUtil.InputExceededException ex) {
            System.out.println("Too many invalid attempts. Returning to previous menu.");
            return;
        }
        if (phone == null) { System.out.println("Cancelled."); return; }
        String sql = "SELECT c.CUSTOMER_ID, c.NAME, c.PHONE_NUMBER, c.ADDRESS, c.ACCOUNT_TYPE, p.PLAN_NAME, p.PLAN_TYPE, p.MONTHLY_RENT, p.FREE_CALL_MINUTES, p.FREE_SMS, p.CALL_RATE, p.SMS_RATE, p.VALIDITY_DAYS, p.PACK_PRICE " +
                    "FROM CUSTOMERS c LEFT JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID WHERE c.PHONE_NUMBER = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int customerId = rs.getInt("CUSTOMER_ID");
                    java.util.List<String> headers = java.util.List.of("Name","Phone","Address","AcctType","Plan","PlanType","Monthly","CallRate","SmsRate","ValidityDays","PackPrice");
                    java.util.List<java.util.List<String>> rows = java.util.List.of(java.util.List.of(
                            rs.getString("NAME"),
                            rs.getString("PHONE_NUMBER"),
                            AdminCommon.nullToEmpty(rs.getString("ADDRESS")),
                            rs.getString("ACCOUNT_TYPE"),
                            rs.getString("PLAN_NAME") == null ? "(none)" : rs.getString("PLAN_NAME"),
                            rs.getString("PLAN_TYPE") == null ? "-" : rs.getString("PLAN_TYPE"),
                            rs.getString("PLAN_NAME") == null ? "-" : DataViewFormatter.money(rs.getDouble("MONTHLY_RENT")),
                            rs.getString("PLAN_NAME") == null ? "-" : DataViewFormatter.money(rs.getDouble("CALL_RATE")),
                            rs.getString("PLAN_NAME") == null ? "-" : DataViewFormatter.money(rs.getDouble("SMS_RATE")),
                            rs.getString("PLAN_NAME") == null ? "-" : (rs.getObject("VALIDITY_DAYS") == null ? "-" : Integer.toString(rs.getInt("VALIDITY_DAYS"))),
                            rs.getString("PLAN_NAME") == null ? "-" : (rs.getObject("PACK_PRICE") == null ? "-" : DataViewFormatter.money(rs.getDouble("PACK_PRICE")))
                    ));
                    System.out.println();
                    System.out.println("CUSTOMER DETAIL");
                    System.out.print(DataViewFormatter.table(headers, rows));

                    String planName = rs.getString("PLAN_NAME");
                    if (planName != null) {
                        String month = AdminCommon.currentBillingMonth();
                        String usageSql = "SELECT USED_CALL_MINUTES, USED_SMS, CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS FROM CUSTOMER_USAGE WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?";
                        try (PreparedStatement ups = conn.prepareStatement(usageSql)) {
                            ups.setInt(1, customerId);
                            ups.setString(2, month);
                            try (ResultSet urs = ups.executeQuery()) {
                                if (urs.next()) {
                                    int usedMin = urs.getInt(1);
                                    int usedSms = urs.getInt(2);
                                    int chgMin = urs.getInt(3);
                                    int chgSms = urs.getInt(4);
                                    double callRate = rs.getDouble("CALL_RATE");
                                    double smsRate = rs.getDouble("SMS_RATE");
                                    double rent = rs.getDouble("MONTHLY_RENT");
                                    double callCost = chgMin * callRate;
                                    double smsCost = chgSms * smsRate;
                                    double estTotal = rent + callCost + smsCost;
                                    java.util.List<String> uHeaders = java.util.List.of("Plan","Month","UsedMin","ChgMin","UsedSMS","ChgSMS","EstTotal");
                                    java.util.List<java.util.List<String>> uRows = java.util.List.of(java.util.List.of(
                                            planName,
                                            DataViewFormatter.monthLabel(month),
                                            Integer.toString(usedMin),
                                            Integer.toString(chgMin),
                                            Integer.toString(usedSms),
                                            Integer.toString(chgSms),
                                            DataViewFormatter.money(estTotal)
                                    ));
                                    System.out.println("USAGE (Current Month)");
                                    System.out.print(DataViewFormatter.table(uHeaders, uRows));
                                } else {
                                    System.out.println("No usage recorded for current month.");
                                }
                            }
                        }
                    } else {
                        System.out.println("No plan assigned; usage not tracked.");
                    }

                    String billSql = "SELECT AMOUNT, BILL_DATE FROM BILLING WHERE CUSTOMER_ID = ? ORDER BY BILL_DATE DESC";
                    try (PreparedStatement bps = conn.prepareStatement(billSql)) {
                        bps.setInt(1, customerId);
                        try (ResultSet brs = bps.executeQuery()) {
                            java.util.List<java.util.List<String>> bRows = new java.util.ArrayList<>();
                            while (brs.next()) {
                                bRows.add(java.util.List.of(
                                        DataViewFormatter.money(brs.getDouble("AMOUNT")),
                                        brs.getDate("BILL_DATE").toString()
                                ));
                            }
                            System.out.println("BILLING HISTORY");
                            if (bRows.isEmpty()) System.out.println("(no bills yet)"); else System.out.print(DataViewFormatter.table(java.util.List.of("Amount","Date"), bRows));
                        }
                    }
                } else {
                    System.out.println("Customer not found.");
                }
            }
        } catch (SQLException e) { System.out.println("Error retrieving customer: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void deleteCustomer(Scanner sc) {
        String phone;
        try {
            phone = InputUtil.readPhoneNumberOrCancel(sc, "Enter Customer Phone Number to delete (or 0 to cancel): ");
        } catch (InputUtil.InputExceededException ex) {
            System.out.println("Too many invalid attempts. Returning to previous menu.");
            return;
        }
        if (phone == null) { System.out.println("Cancelled."); return; }
        String findSql = "SELECT CUSTOMER_ID, NAME FROM CUSTOMERS WHERE PHONE_NUMBER = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement findPs = conn.prepareStatement(findSql)) {
            findPs.setString(1, phone);
            try (ResultSet rs = findPs.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Customer not found.");
                    return;
                }
                int customerId = rs.getInt("CUSTOMER_ID");
                String name = rs.getString("NAME");
                try {
                    conn.setAutoCommit(false);
                    try (PreparedStatement delBills = conn.prepareStatement("DELETE FROM BILLING WHERE CUSTOMER_ID = ?")) {
                        delBills.setInt(1, customerId);
                        delBills.executeUpdate();
                    }
                    try (PreparedStatement delCustomer = conn.prepareStatement("DELETE FROM CUSTOMERS WHERE CUSTOMER_ID = ?")) {
                        delCustomer.setInt(1, customerId);
                        int rows = delCustomer.executeUpdate();
                        if (rows > 0) {
                            conn.commit();
                            System.out.println("Customer '" + name + "' (phone " + phone + ") deleted successfully.");
                        } else {
                            conn.rollback();
                            System.out.println("Deletion failed (customer not found).");
                        }
                    }
                } catch (SQLException txEx) {
                    try { conn.rollback(); } catch (SQLException ignore) {}
                    System.out.println("Error deleting customer: " + txEx.getMessage());
                } finally {
                    try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static int countCustomers() {
        String sql = "SELECT COUNT(*) FROM CUSTOMERS";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }
}
