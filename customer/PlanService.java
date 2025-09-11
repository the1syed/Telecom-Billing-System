package customer;

import java.sql.*;
import java.util.*;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class PlanService {

    static List<CustomerService.PlanOption> fetchPlansByType(String type) {
        List<CustomerService.PlanOption> list = new ArrayList<>();
        String sql = "SELECT PLAN_ID, PLAN_NAME, PLAN_TYPE, NVL(MONTHLY_RENT,0) AS MONTHLY_RENT, NVL(CALL_RATE,0) AS CALL_RATE, FREE_SMS, FREE_CALL_MINUTES, NVL(SMS_RATE,0) AS SMS_RATE, VALIDITY_DAYS, PACK_PRICE FROM PLANS WHERE PLAN_TYPE = ? ORDER BY PLAN_ID";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CustomerService.PlanOption(
                            rs.getInt("PLAN_ID"),
                            rs.getString("PLAN_NAME"),
                            rs.getString("PLAN_TYPE"),
                            rs.getDouble("MONTHLY_RENT"),
                            rs.getDouble("CALL_RATE"),
                            rs.getInt("FREE_SMS"),
                            rs.getInt("FREE_CALL_MINUTES"),
                            rs.getDouble("SMS_RATE"),
                            getNullableInt(rs, "VALIDITY_DAYS"),
                            getNullableDouble(rs, "PACK_PRICE")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading plans: " + e.getMessage());
        }
        return list;
    }

    static List<CustomerService.PlanOption> fetchPlans() {
        List<CustomerService.PlanOption> list = new ArrayList<>();
        String sql = "SELECT PLAN_ID, PLAN_NAME, PLAN_TYPE, NVL(MONTHLY_RENT,0) AS MONTHLY_RENT, NVL(CALL_RATE,0) AS CALL_RATE, FREE_SMS, FREE_CALL_MINUTES, NVL(SMS_RATE,0) AS SMS_RATE, VALIDITY_DAYS, PACK_PRICE FROM PLANS ORDER BY PLAN_ID";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CustomerService.PlanOption(
                        rs.getInt("PLAN_ID"),
                        rs.getString("PLAN_NAME"),
                        rs.getString("PLAN_TYPE"),
                        rs.getDouble("MONTHLY_RENT"),
                        rs.getDouble("CALL_RATE"),
                        rs.getInt("FREE_SMS"),
                        rs.getInt("FREE_CALL_MINUTES"),
                        rs.getDouble("SMS_RATE"),
                        getNullableInt(rs, "VALIDITY_DAYS"),
                        getNullableDouble(rs, "PACK_PRICE")));
            }
        } catch (SQLException e) {
            System.out.println("Error loading plans: " + e.getMessage());
        }
        return list;
    }

    static CustomerService.PlanOption fetchPlanForCustomer(int customerId) {
        String sql = "SELECT p.PLAN_ID, p.PLAN_NAME, p.PLAN_TYPE, NVL(p.MONTHLY_RENT,0) MONTHLY_RENT, NVL(p.CALL_RATE,0) CALL_RATE, p.FREE_SMS, p.FREE_CALL_MINUTES, NVL(p.SMS_RATE,0) SMS_RATE, p.VALIDITY_DAYS, p.PACK_PRICE "
                +
                "FROM CUSTOMERS c JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID WHERE c.CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CustomerService.PlanOption(
                            rs.getInt("PLAN_ID"),
                            rs.getString("PLAN_NAME"),
                            rs.getString("PLAN_TYPE"),
                            rs.getDouble("MONTHLY_RENT"),
                            rs.getDouble("CALL_RATE"),
                            rs.getInt("FREE_SMS"),
                            rs.getInt("FREE_CALL_MINUTES"),
                            rs.getDouble("SMS_RATE"),
                            getNullableInt(rs, "VALIDITY_DAYS"),
                            getNullableDouble(rs, "PACK_PRICE"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching plan: " + e.getMessage());
        }
        return null;
    }

    static void viewMyPlan(int customerId) {
        Scanner sc = new Scanner(System.in);
        String query = "SELECT p.PLAN_NAME, p.PLAN_TYPE, p.MONTHLY_RENT, p.CALL_RATE, p.FREE_SMS, p.FREE_CALL_MINUTES, p.SMS_RATE, p.VALIDITY_DAYS, p.PACK_PRICE, c.ACCOUNT_TYPE "
                +
                "FROM CUSTOMERS c JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID WHERE c.CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String planType = rs.getString("PLAN_TYPE");
                    String accountType = rs.getString("ACCOUNT_TYPE");
                    if ("PREPAID".equalsIgnoreCase(planType)) {
                        Integer validity = getNullableInt(rs, "VALIDITY_DAYS");
                        Double price = getNullableDouble(rs, "PACK_PRICE");
                        java.util.List<String> headers = java.util.List.of("Plan", "Type", "ValidityDays", "PackPrice",
                                "Status");
                        String status = RechargeService.prepaidActiveStatus(customerId);
                        java.util.List<java.util.List<String>> rows = java.util.List.of(java.util.List.of(
                                rs.getString("PLAN_NAME"), planType + "/" + accountType,
                                validity == null ? "-" : validity.toString(),
                                price == null ? "-" : DataViewFormatter.money(price), status));
                        System.out.println();
                        System.out.print(DataViewFormatter.table(headers, rows));
                        System.out.println("Note: Prepaid packs offer unlimited calls and SMS within validity. If expired, please recharge.");
                    } else {
                        java.util.List<String> headers = java.util.List.of("Plan", "Type", "Monthly", "CallRate",
                                "SmsRate");
                        java.util.List<java.util.List<String>> rows = java.util.List.of(java.util.List.of(
                                rs.getString("PLAN_NAME"),
                                planType + "/" + accountType,
                                DataViewFormatter.money(rs.getDouble("MONTHLY_RENT")),
                                DataViewFormatter.money(rs.getDouble("CALL_RATE")),
                                DataViewFormatter.money(rs.getDouble("SMS_RATE"))));
                        System.out.println();
                        System.out.print(DataViewFormatter.table(headers, rows));
                        String nextMonth = nextBillingMonth();
                        String ssql = "SELECT spc.CHANGE_TYPE, p.PLAN_NAME FROM SCHEDULED_PLAN_CHANGES spc JOIN PLANS p ON spc.NEW_PLAN_ID = p.PLAN_ID WHERE spc.CUSTOMER_ID = ? AND spc.EFFECTIVE_MONTH = ? FETCH FIRST 1 ROWS ONLY";
                        try (Connection conn2 = DBConnection.getConnection(); PreparedStatement ps2 = conn2.prepareStatement(ssql)) {
                            ps2.setInt(1, customerId);
                            ps2.setString(2, nextMonth);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    System.out.println("Scheduled change next cycle: " + rs2.getString(1) + " to '" + rs2.getString(2) + "'");
                                }
                            }
                        } catch (SQLException ignored) {}
                    }
                } else {
                    System.out.println("No plan assigned to you.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving plan: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void changePlan(int customerId, Scanner sc) {
        String accountType = AccountService.getAccountType(customerId);
        if (accountType == null) {
            System.out.println("Account not found.");
            return;
        }
        if ("PREPAID".equals(accountType)) {
            System.out.println("Prepaid accounts change plan via recharge; multiple recharges extend validity.");
            String[] yn = {"Go to Recharge", "Cancel"};
            int sel;
            try { sel = InputUtil.readChoice(sc, "Continue?", yn, false); }
            catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            if (sel == 1) { RechargeService.rechargePrepaid(customerId, sc, null); }
            return;
        }
    List<CustomerService.PlanOption> plans = fetchPlansByType(accountType);
        if (plans.isEmpty()) {
            System.out.println("No plans available.");
            return;
        }
        System.out.println();
        java.util.List<String> headers = "PREPAID".equals(accountType)
                ? java.util.List.of("Choice", "Name", "Type", "ValidityDays", "PackPrice")
                : java.util.List.of("Choice", "Name", "Type", "Monthly", "CallRate", "SmsRate");
        java.util.List<java.util.List<String>> tableRows = new java.util.ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            CustomerService.PlanOption p = plans.get(i);
            if ("PREPAID".equals(accountType)) {
                tableRows.add(java.util.List.of(
                        Integer.toString(i + 1), p.name, p.planType,
                        p.validityDays == null ? "-" : Integer.toString(p.validityDays),
                        p.packPrice == null ? "-" : DataViewFormatter.money(p.packPrice)));
            } else {
                tableRows.add(java.util.List.of(
                        Integer.toString(i + 1), p.name, p.planType,
                        DataViewFormatter.money(p.monthlyRent),
                        DataViewFormatter.money(p.callRate),
                        DataViewFormatter.money(p.smsRate)));
            }
        }
        System.out.print(DataViewFormatter.table(headers, tableRows));
        int choice;
        while (true) {
            try {
                choice = InputUtil.readInt(sc, "Select a plan (1-" + plans.size() + ") or 0 to cancel: ", 0,
                        plans.size());
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Cancelled.");
                return;
            }
            if (choice == 0) {
                System.out.println("Cancelled.");
                return;
            }
            if (choice >= 1 && choice <= plans.size())
                break;
        }
    int selectedPlanId = plans.get(choice - 1).id;

        if ("PREPAID".equals(accountType)) {
            System.out.println("Switching prepaid pack requires payment.");
            String[] yn = {"Proceed to recharge", "Cancel"};
            int sel;
            try {
                sel = InputUtil.readChoice(sc, "Continue?", yn, false);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Cancelled.");
                return;
            }
            if (sel == 1) {
                RechargeService.rechargePrepaid(customerId, sc, selectedPlanId);
            } else {
                System.out.println("No payment made. Plan not changed.");
                InputUtil.pressEnterToContinue(sc);
            }
            return;
        }

        String sql = "UPDATE CUSTOMERS SET PLAN_ID = ? WHERE CUSTOMER_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedPlanId);
            ps.setInt(2, customerId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("Plan updated.");
            else
                System.out.println("Update failed.");
        } catch (SQLException e) {
            System.out.println("Error changing plan: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void schedulePlanChange(int customerId, Scanner sc, boolean isUpgrade) {
        String acc = AccountService.getAccountType(customerId);
        if (!"POSTPAID".equals(acc)) {
            System.out.println("This option is only for POSTPAID accounts.");
            return;
        }
    CustomerService.PlanOption current = fetchPlanForCustomer(customerId);
        if (current == null) {
            System.out.println("No current plan. Please pick a postpaid plan first.");
            changePlan(customerId, sc);
            return;
        }
    List<CustomerService.PlanOption> plans = fetchPlansByType("POSTPAID");
        if (plans.isEmpty()) { System.out.println("No postpaid plans found."); return; }
        System.out.println();
        System.out.println("POSTPAID PLANS");
        java.util.List<String> headers = java.util.List.of("Choice","Name","Monthly","CallRate","SmsRate");
        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            CustomerService.PlanOption p = plans.get(i);
            rows.add(java.util.List.of(
                Integer.toString(i+1), p.name,
                DataViewFormatter.money(p.monthlyRent),
                DataViewFormatter.money(p.callRate),
                DataViewFormatter.money(p.smsRate)
            ));
        }
        System.out.print(DataViewFormatter.table(headers, rows));
        int choice;
        try { choice = InputUtil.readInt(sc, "Select a plan (1-"+plans.size()+") or 0 to cancel: ", 0, plans.size()); }
        catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
        if (choice == 0) { System.out.println("Cancelled."); return; }
    CustomerService.PlanOption target = plans.get(choice-1);
        if (target.id == current.id) { System.out.println("You're already on this plan."); return; }

    String nextMonth = nextBillingMonth();
        if (isUpgrade) {
            String[] opts = {"Apply now (prorated difference added to current bill)", "From next billing cycle"};
            int sel;
            try { sel = InputUtil.readChoice(sc, "Upgrade timing", opts, false); }
            catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            if (sel == 1) {
                double extra = proratedDifference(current.monthlyRent, target.monthlyRent);
                try (Connection conn = DBConnection.getConnection()) {
                    try (PreparedStatement up = conn.prepareStatement("UPDATE CUSTOMERS SET PLAN_ID = ? WHERE CUSTOMER_ID = ?")) {
                        up.setInt(1, target.id); up.setInt(2, customerId); up.executeUpdate();
                    }
                    if (extra > 0.0) {
                        try (PreparedStatement ins = conn.prepareStatement("INSERT INTO BILLING (CUSTOMER_ID, AMOUNT) VALUES (?, ?)")) {
                            ins.setInt(1, customerId); ins.setDouble(2, extra); ins.executeUpdate();
                        }
                        System.out.println("Upgrade applied now. Prorated charge added: " + DataViewFormatter.money(extra));
                    } else {
                        System.out.println("Upgrade applied now. No prorated charge.");
                    }
                } catch (SQLException e) {
                    System.out.println("Failed to apply upgrade: " + e.getMessage());
                }
            } else {
                insertScheduledChange(customerId, target.id, "UPGRADE", nextMonth);
            }
        } else {
            System.out.println("Downgrades take effect next billing cycle.");
            insertScheduledChange(customerId, target.id, "DOWNGRADE", nextMonth);
        }
        InputUtil.pressEnterToContinue(sc);
    }

    private static void insertScheduledChange(int customerId, int newPlanId, String type, String effectiveMonth) {
        String sql = "INSERT INTO SCHEDULED_PLAN_CHANGES (CUSTOMER_ID, NEW_PLAN_ID, CHANGE_TYPE, EFFECTIVE_MONTH) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, newPlanId);
            ps.setString(3, type);
            ps.setString(4, effectiveMonth);
            ps.executeUpdate();
            System.out.println("Scheduled " + type.toLowerCase() + " for " + DataViewFormatter.monthLabel(effectiveMonth) + ".");
        } catch (SQLException e) {
            System.out.println("Error scheduling plan change: " + e.getMessage());
        }
    }

    static String nextBillingMonth() {
        java.time.LocalDate now = java.time.LocalDate.now().withDayOfMonth(1).plusMonths(1);
        return String.format("%04d%02d", now.getYear(), now.getMonthValue());
    }

    static double proratedDifference(double currentMonthly, double newMonthly) {
        java.time.LocalDate today = java.time.LocalDate.now();
        int dim = today.lengthOfMonth();
        int remaining = dim - today.getDayOfMonth() + 1; // include today
        double fraction = Math.max(0.0, Math.min(1.0, ((double) remaining) / dim));
        double delta = newMonthly - currentMonthly;
        return delta > 0 ? delta * fraction : 0.0;
    }

    // Apply a scheduled plan change if there's one effective for the current month
    static void applyScheduledPlanChangeIfDue(int customerId) {
        String currentMonth = String.format("%04d%02d", java.time.LocalDate.now().getYear(), java.time.LocalDate.now().getMonthValue());
        String findSql = "SELECT NEW_PLAN_ID, CHANGE_TYPE FROM SCHEDULED_PLAN_CHANGES WHERE CUSTOMER_ID = ? AND EFFECTIVE_MONTH = ? FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setInt(1, customerId);
            ps.setString(2, currentMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newPlanId = rs.getInt(1);
                    String type = rs.getString(2);
                    try (PreparedStatement up = conn.prepareStatement("UPDATE CUSTOMERS SET PLAN_ID = ? WHERE CUSTOMER_ID = ?")) {
                        up.setInt(1, newPlanId); up.setInt(2, customerId); up.executeUpdate();
                    }
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM SCHEDULED_PLAN_CHANGES WHERE CUSTOMER_ID = ? AND EFFECTIVE_MONTH = ?")) {
                        del.setInt(1, customerId); del.setString(2, currentMonth); del.executeUpdate();
                    }
                    // Optional informational message
                    String planName = null;
                    try (PreparedStatement pn = conn.prepareStatement("SELECT PLAN_NAME FROM PLANS WHERE PLAN_ID = ?")) {
                        pn.setInt(1, newPlanId);
                        try (ResultSet pr = pn.executeQuery()) { if (pr.next()) planName = pr.getString(1); }
                    } catch (SQLException ignored) {}
                    System.out.println("Applied scheduled " + (type == null ? "change" : type.toLowerCase()) + " for " + DataViewFormatter.monthLabel(currentMonth) + (planName == null ? "." : (": " + planName)));
                }
            }
        } catch (SQLException e) {
            // Non-fatal; just log
            System.out.println("Error applying scheduled plan change: " + e.getMessage());
        }
    }

    private static Integer getNullableInt(ResultSet rs, String column) {
        try {
            Object o = rs.getObject(column);
            if (o == null)
                return null;
            if (o instanceof Number n)
                return n.intValue();
            if (o instanceof String s && !s.isBlank())
                return Integer.valueOf(s.trim());
        } catch (SQLException | NumberFormatException ignored) {
        }
        return null;
    }

    private static Double getNullableDouble(ResultSet rs, String column) {
        try {
            Object o = rs.getObject(column);
            if (o == null)
                return null;
            if (o instanceof Number n)
                return n.doubleValue();
            if (o instanceof String s && !s.isBlank())
                return Double.valueOf(s.trim());
        } catch (SQLException | NumberFormatException ignored) {
        }
        return null;
    }
}
