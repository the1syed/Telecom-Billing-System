package admin;

import java.sql.*;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AdminBillingService {

    static void viewAllBills() {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        String query = "SELECT b.BILL_ID, c.NAME, c.PHONE_NUMBER, b.AMOUNT, b.BILL_DATE FROM BILLING b JOIN CUSTOMERS c ON b.CUSTOMER_ID = c.CUSTOMER_ID ORDER BY b.BILL_DATE DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(java.util.List.of(
                        Integer.toString(rs.getInt("BILL_ID")),
                        rs.getString("NAME"),
                        rs.getString("PHONE_NUMBER"),
                        DataViewFormatter.money(rs.getDouble("AMOUNT")),
                        rs.getDate("BILL_DATE").toString()
                ));
            }
            System.out.println();
            System.out.println("BILLS");
            System.out.print(DataViewFormatter.table(java.util.List.of("BillId", "Customer", "Phone", "Amount", "Date"), rows));
        } catch (Exception e) { System.out.println("Error loading bills: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void generateBill(java.util.Scanner sc) {
        try (Connection conn = DBConnection.getConnection()) {
            int customerId;
            double amount;
            try {
                customerId = InputUtil.readInt(sc, "Enter Customer ID: ", 1, Integer.MAX_VALUE);
                amount = InputUtil.readDouble(sc, "Enter Bill Amount: ", 0.0);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Aborting bill generation.");
                return;
            }

            String query = "INSERT INTO BILLING (CUSTOMER_ID, AMOUNT) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, customerId);
            ps.setDouble(2, amount);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Bill generated successfully!");
            } else {
                System.out.println("Failed to generate bill.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void revenueReport() {
        String query = "SELECT SUM(AMOUNT) AS TOTAL_REVENUE FROM BILLING";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("\nTotal Revenue: " + rs.getDouble("TOTAL_REVENUE"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        java.util.Scanner sc = new java.util.Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }

    static void generateMonthlyBillsForAllPostpaid() {
        String month = AdminCommon.currentBillingMonth();
        String fetch = "SELECT c.CUSTOMER_ID, p.MONTHLY_RENT, p.CALL_RATE, p.SMS_RATE " +
                "FROM CUSTOMERS c JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID " +
                "WHERE c.ACCOUNT_TYPE = 'POSTPAID' AND p.PLAN_TYPE = 'POSTPAID'";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(fetch)) {
            int billed = 0;
            while (rs.next()) {
                int cid = rs.getInt(1);
                double rent = rs.getDouble(2);
                double callRate = rs.getDouble(3);
                double smsRate = rs.getDouble(4);
                int chgMin = 0, chgSms = 0;
                try (PreparedStatement ups = conn.prepareStatement("SELECT CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS FROM CUSTOMER_USAGE WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?")) {
                    ups.setInt(1, cid); ups.setString(2, month);
                    try (ResultSet urs = ups.executeQuery()) { if (urs.next()) { chgMin = urs.getInt(1); chgSms = urs.getInt(2);} }
                }
                double amount = rent + chgMin * callRate + chgSms * smsRate;
                try (PreparedStatement ins = conn.prepareStatement("INSERT INTO BILLING (CUSTOMER_ID, AMOUNT) VALUES (?, ?)")) {
                    ins.setInt(1, cid); ins.setDouble(2, amount); ins.executeUpdate();
                    billed++;
                }
                try (PreparedStatement clr = conn.prepareStatement("UPDATE CUSTOMER_USAGE SET USED_CALL_MINUTES = 0, USED_SMS = 0, CHARGEABLE_CALL_MINUTES = 0, CHARGEABLE_SMS = 0 WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?")) {
                    clr.setInt(1, cid); clr.setString(2, month); clr.executeUpdate();
                }
            }
            java.time.LocalDate d = java.time.LocalDate.now().withDayOfMonth(1).plusMonths(1);
            String nextMonth = String.format("%04d%02d", d.getYear(), d.getMonthValue());
            String spcSql = "SELECT CUSTOMER_ID, NEW_PLAN_ID FROM SCHEDULED_PLAN_CHANGES WHERE EFFECTIVE_MONTH = ?";
            try (PreparedStatement ps = conn.prepareStatement(spcSql)) {
                ps.setString(1, nextMonth);
                try (ResultSet prs = ps.executeQuery()) {
                    while (prs.next()) {
                        int cid = prs.getInt(1);
                        int np = prs.getInt(2);
                        try (PreparedStatement up = conn.prepareStatement("UPDATE CUSTOMERS SET PLAN_ID = ? WHERE CUSTOMER_ID = ?")) {
                            up.setInt(1, np); up.setInt(2, cid); up.executeUpdate();
                        }
                    }
                }
            }
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM SCHEDULED_PLAN_CHANGES WHERE EFFECTIVE_MONTH = ?")) {
                del.setString(1, nextMonth);
                del.executeUpdate();
            }
            System.out.println("Generated bills for " + billed + " postpaid customer(s) for month " + DataViewFormatter.monthLabel(month) + ". Applied scheduled plan changes for " + DataViewFormatter.monthLabel(nextMonth) + ".");
        } catch (SQLException e) {
            System.out.println("Error generating monthly bills: " + e.getMessage());
        }
    }
}
