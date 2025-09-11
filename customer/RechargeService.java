package customer;

import java.sql.*;
import java.util.*;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class RechargeService {

    static boolean hasActivePrepaid(int customerId) {
        String sql = "SELECT 1 FROM RECHARGES WHERE CUSTOMER_ID = ? AND TRUNC(SYSDATE) BETWEEN TRUNC(VALID_FROM) AND TRUNC(VALID_TO) ORDER BY RECHARGE_ID DESC FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    static String prepaidActiveStatus(int customerId) {
        String sql = "SELECT VALID_FROM, VALID_TO FROM RECHARGES WHERE CUSTOMER_ID = ? ORDER BY RECHARGE_ID DESC FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date vf = rs.getDate(1);
                    java.sql.Date vt = rs.getDate(2);
                    java.time.LocalDate today = java.time.LocalDate.now();
                    boolean active = !today.isBefore(vf.toLocalDate()) && !today.isAfter(vt.toLocalDate());
                    return (active ? "Active" : "Expired") + " (" + vf.toString() + " to " + vt.toString() + ")";
                }
            }
        } catch (SQLException ignored) {
        }
        return "No recharges yet";
    }

    static void rechargePrepaid(int customerId, Scanner sc) {
        rechargePrepaid(customerId, sc, null);
    }

    static void rechargePrepaid(int customerId, Scanner sc, Integer preferredPlanId) {
        String accountType = AccountService.getAccountType(customerId);
        if (!"PREPAID".equals(accountType)) {
            System.out.println("Recharge is only for PREPAID accounts.");
            return;
        }
        List<CustomerService.PlanOption> packs = PlanService.fetchPlansByType("PREPAID");
        if (packs.isEmpty()) {
            System.out.println("No prepaid packs available.");
            return;
        }

        CustomerService.PlanOption chosen = null;
        if (preferredPlanId != null) {
            for (CustomerService.PlanOption p : packs) {
                if (p.id == preferredPlanId) { chosen = p; break; }
            }
        }

        if (chosen == null) {
            System.out.println();
            java.util.List<String> headers = java.util.List.of("Choice", "Name", "ValidityDays", "PackPrice");
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            for (int i = 0; i < packs.size(); i++) {
                CustomerService.PlanOption p = packs.get(i);
                rows.add(java.util.List.of(
                        Integer.toString(i + 1), p.name,
                        p.validityDays == null ? "-" : Integer.toString(p.validityDays),
                        p.packPrice == null ? "-" : DataViewFormatter.money(p.packPrice)));
            }
            System.out.print(DataViewFormatter.table(headers, rows));
            int sel;
            try {
                sel = InputUtil.readInt(sc, "Select a pack (1-" + packs.size() + "): ", 1, packs.size());
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Cancelling recharge.");
                return;
            }
            chosen = packs.get(sel - 1);
        } else {
            System.out.println("Using selected pack: " + chosen.name + " (" +
                    (chosen.validityDays == null ? "-" : chosen.validityDays + " days") + ", " +
                    (chosen.packPrice == null ? "-" : DataViewFormatter.money(chosen.packPrice)) + ")");
        }

        if (chosen.packPrice == null || chosen.validityDays == null) {
            System.out.println("Invalid pack selection.");
            return;
        }

        String payMethod;
        {
            String[] methods = {"UPI", "CARD", "NETBANKING"};
            int sel;
            try { sel = InputUtil.readChoice(sc, "Payment Method", methods, true); }
            catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts. Cancelling recharge."); return; }
            if (sel == 0) { System.out.println("Cancelled."); return; }
            payMethod = methods[sel - 1];
        }
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement upd = conn
                    .prepareStatement("UPDATE CUSTOMERS SET PLAN_ID = ? WHERE CUSTOMER_ID = ?")) {
                upd.setInt(1, chosen.id);
                upd.setInt(2, customerId);
                upd.executeUpdate();
            }
            java.time.LocalDate today = java.time.LocalDate.now();
            java.sql.Date lastVt = null;
            try (PreparedStatement q = conn.prepareStatement("SELECT MAX(VALID_TO) FROM RECHARGES WHERE CUSTOMER_ID = ?")) {
                q.setInt(1, customerId);
                try (ResultSet rs = q.executeQuery()) { if (rs.next()) lastVt = rs.getDate(1); }
            }
            java.time.LocalDate start = today;
            if (lastVt != null) {
                java.time.LocalDate last = lastVt.toLocalDate();
                if (!today.isAfter(last)) { start = last.plusDays(1); }
            }
            java.time.LocalDate end = start.plusDays(chosen.validityDays);
            String ins = "INSERT INTO RECHARGES (CUSTOMER_ID, PLAN_ID, AMOUNT, PAYMENT_METHOD, VALID_FROM, VALID_TO) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                ps.setInt(1, customerId);
                ps.setInt(2, chosen.id);
                ps.setDouble(3, chosen.packPrice);
                ps.setString(4, payMethod);
                ps.setDate(5, java.sql.Date.valueOf(start));
                ps.setDate(6, java.sql.Date.valueOf(end));
                ps.executeUpdate();
            }
            System.out.println("✅ Recharge successful. Valid from " + start + " to " + end + ". Amount: ₹"
                    + DataViewFormatter.money(chosen.packPrice));
        } catch (SQLException e) {
            System.out.println("Recharge failed: " + e.getMessage());
        }
        InputUtil.pressEnterToContinue(sc);
    }

    static void viewRechargeHistory(int customerId) {
        String acc = AccountService.getAccountType(customerId);
        if (acc == null) { System.out.println("Account not found."); return; }
        if (!"PREPAID".equals(acc)) {
            System.out.println("Recharge history is only for PREPAID accounts.");
            Scanner sc = new Scanner(System.in);
            InputUtil.pressEnterToContinue(sc);
            return;
        }
        String sql = "SELECT r.AMOUNT, r.PAYMENT_METHOD, r.VALID_FROM, r.VALID_TO, p.PLAN_NAME " +
                "FROM RECHARGES r LEFT JOIN PLANS p ON r.PLAN_ID = p.PLAN_ID " +
                "WHERE r.CUSTOMER_ID = ? ORDER BY r.RECHARGE_ID DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    rows.add(java.util.List.of(
                            rs.getString("PLAN_NAME") == null ? "(unknown)" : rs.getString("PLAN_NAME"),
                            DataViewFormatter.money(rs.getDouble("AMOUNT")),
                            rs.getString("PAYMENT_METHOD"),
                            rs.getDate("VALID_FROM").toString(),
                            rs.getDate("VALID_TO").toString()
                    ));
                }
                System.out.println();
                if (rows.isEmpty()) {
                    System.out.println("No recharges yet.");
                } else {
                    System.out.print(DataViewFormatter.table(java.util.List.of("Pack","Amount","Method","From","To"), rows));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading recharge history: " + e.getMessage());
        }
        Scanner sc = new Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }
}
