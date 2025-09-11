package customer;

import java.sql.*;
import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class BillingService {

    static String currentBillingMonth() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%04d%02d", now.getYear(), now.getMonthValue());
    }

    static void viewMyBills(int customerId, java.util.Scanner sc) {
        String query = "SELECT AMOUNT, BILL_DATE FROM BILLING WHERE CUSTOMER_ID = ? ORDER BY BILL_DATE DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    rows.add(java.util.List.of(
                            DataViewFormatter.money(rs.getDouble("AMOUNT")),
                            rs.getDate("BILL_DATE").toString()));
                }
                System.out.println();
                System.out.print(DataViewFormatter.table(java.util.List.of("Amount", "Date"), rows));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving bills: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void generateMyBill(int customerId) {
        String accountType = AccountService.getAccountType(customerId);
        if ("PREPAID".equals(accountType)) {
            System.out.println("Prepaid accounts don't generate monthly bills. Please check Recharge History.");
            java.util.Scanner sc = new java.util.Scanner(System.in);
            InputUtil.pressEnterToContinue(sc);
            return;
        }
        CustomerService.PlanOption plan = PlanService.fetchPlanForCustomer(customerId);
        if (plan == null) { System.out.println("No plan assigned; cannot generate bill."); return; }
        String month = currentBillingMonth();
        String usageSql = "SELECT CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS FROM CUSTOMER_USAGE WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(usageSql)) {
            ps.setInt(1, customerId);
            ps.setString(2, month);
            int chargeMin = 0, chargeSms = 0;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { chargeMin = rs.getInt(1); chargeSms = rs.getInt(2); }
            }
            double amount = plan.monthlyRent + chargeMin * plan.callRate + chargeSms * plan.smsRate;
            try (PreparedStatement ins = conn
                    .prepareStatement("INSERT INTO BILLING (CUSTOMER_ID, AMOUNT) VALUES (?, ?)")) {
                ins.setInt(1, customerId);
                ins.setDouble(2, amount);
                ins.executeUpdate();
            }
            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE CUSTOMER_USAGE SET USED_CALL_MINUTES = 0, USED_SMS = 0, CHARGEABLE_CALL_MINUTES = 0, CHARGEABLE_SMS = 0 WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?")) {
                upd.setInt(1, customerId);
                upd.setString(2, month);
                upd.executeUpdate();
            }
            System.out.println("✅ Bill generated for month " + DataViewFormatter.monthLabel(month) + ". Amount: ₹"
                    + String.format("%.2f", amount));
        } catch (SQLException e) { System.out.println("Error generating bill: " + e.getMessage()); }
        java.util.Scanner sc = new java.util.Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }
}
