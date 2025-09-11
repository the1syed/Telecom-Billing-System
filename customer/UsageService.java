package customer;

import java.sql.*;
import java.util.Scanner;
import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class UsageService {

    static class UsageSnapshot { int usedMin; int usedSms; int chargeMin; int chargeSms; }

    static void ensureUsageRow(Connection conn, int customerId, String month) throws SQLException {
        String sql = "MERGE INTO CUSTOMER_USAGE tgt USING (SELECT ? AS CID, ? AS BM FROM dual) src " +
                "ON (tgt.CUSTOMER_ID = src.CID AND tgt.BILLING_MONTH = src.BM) " +
                "WHEN NOT MATCHED THEN INSERT (CUSTOMER_ID, BILLING_MONTH, USED_CALL_MINUTES, USED_SMS, CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS) VALUES (src.CID, src.BM, 0, 0, 0, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, customerId); ps.setString(2, month); ps.executeUpdate(); }
    }

    static String currentBillingMonth() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%04d%02d", now.getYear(), now.getMonthValue());
    }

    static UsageSnapshot loadUsageSnapshot(int customerId, String month) {
        String sql = "SELECT USED_CALL_MINUTES, USED_SMS, CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS FROM CUSTOMER_USAGE WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UsageSnapshot us = new UsageSnapshot();
                    us.usedMin = rs.getInt(1); us.usedSms = rs.getInt(2); us.chargeMin = rs.getInt(3); us.chargeSms = rs.getInt(4);
                    return us;
                }
            }
        } catch (SQLException ignored) {}
        return null;
    }

    static void simulateCall(int customerId, Scanner sc) {
        CustomerService.PlanOption plan = PlanService.fetchPlanForCustomer(customerId);
        String accountType = AccountService.getAccountType(customerId);
        if (plan == null) {
            if ("PREPAID".equals(accountType)) {
                System.out.println("You don't have a plan assigned. Recharge a prepaid pack to start calling.");
                String[] yn = {"Recharge now", "Cancel"};
                int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
                if (sel == 1) { RechargeService.rechargePrepaid(customerId, sc, null); plan = PlanService.fetchPlanForCustomer(customerId); if (plan == null) { System.out.println("No plan assigned. Call cancelled."); return; } } else { return; }
            } else {
                System.out.println("You don't have a plan assigned. Please pick a postpaid plan first.");
                String[] yn = {"Choose plan", "Cancel"};
                int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
                if (sel == 1) { PlanService.changePlan(customerId, sc); plan = PlanService.fetchPlanForCustomer(customerId); if (plan == null) { System.out.println("No plan assigned. Call cancelled."); return; } } else { return; }
            }
        }
        if ("PREPAID".equals(accountType) && !RechargeService.hasActivePrepaid(customerId)) {
            System.out.println("No active prepaid pack. You must recharge before making calls.");
            String[] yn = {"Recharge now", "Cancel"};
            int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            if (sel == 1) { RechargeService.rechargePrepaid(customerId, sc, plan.id); if (!RechargeService.hasActivePrepaid(customerId)) { System.out.println("Recharge not completed. Call cancelled."); return; } } else { return; }
        }
        String month = currentBillingMonth();
        UsageSnapshot snap = loadUsageSnapshot(customerId, month);
        if (snap == null) snap = new UsageSnapshot();
        System.out.println();
        System.out.println("CALL SIMULATION");
        System.out.println("Plan: " + plan.name + "  Month: " + DataViewFormatter.monthLabel(month));
        if ("PREPAID".equals(accountType)) {
            java.util.List<String> headersPre = java.util.List.of("Type", "Free", "Used", "FreeLeft", "Chargeable",
                    "Rate", "CostSoFar");
            double callCostSoFar = snap.chargeMin * plan.callRate;
            double smsCostSoFar = snap.chargeSms * plan.smsRate;
            java.util.List<java.util.List<String>> rowsPre = java.util.List.of(
                    java.util.List.of("Calls", Integer.toString(plan.freeCallMinutes), Integer.toString(snap.usedMin),
                            Integer.toString(Math.max(0, plan.freeCallMinutes - snap.usedMin)),
                            Integer.toString(snap.chargeMin), DataViewFormatter.money(plan.callRate),
                            DataViewFormatter.money(callCostSoFar)),
                    java.util.List.of("SMS", Integer.toString(plan.freeSms), Integer.toString(snap.usedSms),
                            Integer.toString(Math.max(0, plan.freeSms - snap.usedSms)),
                            Integer.toString(snap.chargeSms), DataViewFormatter.money(plan.smsRate),
                            DataViewFormatter.money(smsCostSoFar)));
            System.out.print(DataViewFormatter.table(headersPre, rowsPre));
        } else {
            java.util.List<String> headersPre = java.util.List.of("Type", "Used", "Chargeable", "Rate", "CostSoFar");
            double callCostSoFar = snap.chargeMin * plan.callRate;
            double smsCostSoFar = snap.chargeSms * plan.smsRate;
            java.util.List<java.util.List<String>> rowsPre = java.util.List.of(
                    java.util.List.of("Calls", Integer.toString(snap.usedMin), Integer.toString(snap.chargeMin),
                            DataViewFormatter.money(plan.callRate), DataViewFormatter.money(callCostSoFar)),
                    java.util.List.of("SMS", Integer.toString(snap.usedSms), Integer.toString(snap.chargeSms),
                            DataViewFormatter.money(plan.smsRate), DataViewFormatter.money(smsCostSoFar)));
            System.out.print(DataViewFormatter.table(headersPre, rowsPre));
        }
        int minutes; try { minutes = InputUtil.readInt(sc, "Enter simulated call duration in minutes (1-300, 0 to cancel): ", 0, 300); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts. Cancelled."); return; }
        if (minutes == 0) { System.out.println("Cancelled."); return; }
        System.out.print("Dialing");
        try { for (int i = 0; i < 3; i++) { Thread.sleep(250); System.out.print("."); } } catch (InterruptedException ignored) {}
        System.out.println("\nCall connected.");
        System.out.println("Simulating conversation...");
        try { Thread.sleep(Math.min(1500, minutes * 60L)); } catch (InterruptedException ignored) {}
        System.out.println("Call ended. Duration: " + minutes + " minute(s). Updating usage...");
        try (Connection conn = DBConnection.getConnection()) {
            ensureUsageRow(conn, customerId, month);
            int incrementalChargeable = "PREPAID".equals(accountType) ? 0 : minutes;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE CUSTOMER_USAGE SET USED_CALL_MINUTES = USED_CALL_MINUTES + ?, CHARGEABLE_CALL_MINUTES = CHARGEABLE_CALL_MINUTES + ? WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?")) {
                ps.setInt(1, minutes); ps.setInt(2, incrementalChargeable); ps.setInt(3, customerId); ps.setString(4, month); ps.executeUpdate();
            }
            int freeConsumedThisCall = "PREPAID".equals(accountType) ? minutes : 0;
            double costThisCall = "PREPAID".equals(accountType) ? 0.0 : incrementalChargeable * plan.callRate;
            System.out.println("Call usage recorded.");
            if ("PREPAID".equals(accountType) && freeConsumedThisCall > 0) { System.out.println("  Free minutes used this call: " + freeConsumedThisCall); }
            if (incrementalChargeable > 0) { System.out.println("  Chargeable minutes this call: " + incrementalChargeable + " @ " + plan.callRate + " = ₹" + String.format("%.2f", costThisCall)); }
            if ("PREPAID".equals(accountType)) { System.out.println("  Remaining free call minutes this month: Unlimited"); }
            showCurrentUsage(conn, customerId, plan, month, accountType);
        } catch (SQLException e) { System.out.println("Error recording call: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void simulateSms(int customerId, Scanner sc) {
        CustomerService.PlanOption plan = PlanService.fetchPlanForCustomer(customerId);
        String accountType = AccountService.getAccountType(customerId);
        if (plan == null) {
            if ("PREPAID".equals(accountType)) {
                System.out.println("You don't have a plan assigned. Recharge a prepaid pack to send SMS.");
                String[] yn = {"Recharge now", "Cancel"};
                int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
                if (sel == 1) { RechargeService.rechargePrepaid(customerId, sc, null); plan = PlanService.fetchPlanForCustomer(customerId); if (plan == null) { System.out.println("No plan assigned. SMS cancelled."); return; } } else { return; }
            } else {
                System.out.println("You don't have a plan assigned. Please pick a postpaid plan first.");
                String[] yn = {"Choose plan", "Cancel"};
                int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
                if (sel == 1) { PlanService.changePlan(customerId, sc); plan = PlanService.fetchPlanForCustomer(customerId); if (plan == null) { System.out.println("No plan assigned. SMS cancelled."); return; } } else { return; }
            }
        }
        if ("PREPAID".equals(accountType) && !RechargeService.hasActivePrepaid(customerId)) {
            System.out.println("No active prepaid pack. You must recharge before sending SMS.");
            String[] yn = {"Recharge now", "Cancel"};
            int sel; try { sel = InputUtil.readChoice(sc, "Proceed?", yn, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            if (sel == 1) { RechargeService.rechargePrepaid(customerId, sc, plan.id); if (!RechargeService.hasActivePrepaid(customerId)) { System.out.println("Recharge not completed. SMS cancelled."); return; } } else { return; }
        }
        String month = currentBillingMonth();
        UsageSnapshot snap = loadUsageSnapshot(customerId, month);
        if (snap == null) snap = new UsageSnapshot();
        System.out.println();
        System.out.println("SMS SIMULATION");
        System.out.println("Plan: " + plan.name + "  Month: " + DataViewFormatter.monthLabel(month));
        if ("PREPAID".equals(accountType)) {
            java.util.List<String> headersPre = java.util.List.of("Type", "Free", "Used", "FreeLeft", "Chargeable",
                    "Rate", "CostSoFar");
            double callCostSoFar = snap.chargeMin * plan.callRate;
            double smsCostSoFar = snap.chargeSms * plan.smsRate;
            java.util.List<java.util.List<String>> rowsPre = java.util.List.of(
                    java.util.List.of("Calls", Integer.toString(plan.freeCallMinutes), Integer.toString(snap.usedMin),
                            Integer.toString(Math.max(0, plan.freeCallMinutes - snap.usedMin)),
                            Integer.toString(snap.chargeMin), DataViewFormatter.money(plan.callRate),
                            DataViewFormatter.money(callCostSoFar)),
                    java.util.List.of("SMS", Integer.toString(plan.freeSms), Integer.toString(snap.usedSms),
                            Integer.toString(Math.max(0, plan.freeSms - snap.usedSms)),
                            Integer.toString(snap.chargeSms), DataViewFormatter.money(plan.smsRate),
                            DataViewFormatter.money(smsCostSoFar)));
            System.out.print(DataViewFormatter.table(headersPre, rowsPre));
        } else {
            java.util.List<String> headersPre = java.util.List.of("Type", "Used", "Chargeable", "Rate", "CostSoFar");
            double callCostSoFar = snap.chargeMin * plan.callRate;
            double smsCostSoFar = snap.chargeSms * plan.smsRate;
            java.util.List<java.util.List<String>> rowsPre = java.util.List.of(
                    java.util.List.of("Calls", Integer.toString(snap.usedMin), Integer.toString(snap.chargeMin),
                            DataViewFormatter.money(plan.callRate), DataViewFormatter.money(callCostSoFar)),
                    java.util.List.of("SMS", Integer.toString(snap.usedSms), Integer.toString(snap.chargeSms),
                            DataViewFormatter.money(plan.smsRate), DataViewFormatter.money(smsCostSoFar)));
            System.out.print(DataViewFormatter.table(headersPre, rowsPre));
        }
        int count; try { count = InputUtil.readInt(sc, "Enter number of SMS to send (1-100, 0 to cancel): ", 0, 100); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts. Cancelled."); return; }
        if (count == 0) { System.out.println("Cancelled."); return; }
        System.out.print("Sending messages");
        try { for (int i = 0; i < Math.min(5, count); i++) { Thread.sleep(120); System.out.print("."); } } catch (InterruptedException ignored) {}
        System.out.println("\nSent " + count + " SMS.");
        try (Connection conn = DBConnection.getConnection()) {
            ensureUsageRow(conn, customerId, month);
            int incrementalChargeable = "PREPAID".equals(accountType) ? 0 : count;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE CUSTOMER_USAGE SET USED_SMS = USED_SMS + ?, CHARGEABLE_SMS = CHARGEABLE_SMS + ? WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?")) {
                ps.setInt(1, count); ps.setInt(2, incrementalChargeable); ps.setInt(3, customerId); ps.setString(4, month); ps.executeUpdate();
            }
            System.out.println("SMS usage recorded.");
            int freeConsumed = "PREPAID".equals(accountType) ? count : 0;
            double batchCost = "PREPAID".equals(accountType) ? 0.0 : incrementalChargeable * plan.smsRate;
            if ("PREPAID".equals(accountType) && freeConsumed > 0) System.out.println("  Free SMS used this batch: " + freeConsumed);
            if (incrementalChargeable > 0) System.out.println("  Chargeable SMS this batch: " + incrementalChargeable + " @ " + plan.smsRate + " = ₹" + String.format("%.2f", batchCost));
            if ("PREPAID".equals(accountType)) System.out.println("  Remaining free SMS this month: Unlimited");
            showCurrentUsage(conn, customerId, plan, month, accountType);
        } catch (SQLException e) { System.out.println("Error recording SMS: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void showCurrentUsage(Connection conn, int customerId, CustomerService.PlanOption plan, String month, String accountType) throws SQLException {
        String sql = "SELECT USED_CALL_MINUTES, USED_SMS, CHARGEABLE_CALL_MINUTES, CHARGEABLE_SMS FROM CUSTOMER_USAGE WHERE CUSTOMER_ID = ? AND BILLING_MONTH = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId); ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int usedMin = rs.getInt(1); int usedSms = rs.getInt(2); int chargeMin = rs.getInt(3); int chargeSms = rs.getInt(4);
                    if ("PREPAID".equals(accountType)) {
                        System.out.println();
                        System.out.println("USAGE (Prepaid, Unlimited within validity) - Plan: " + plan.name);
                        java.util.List<String> headers = java.util.List.of("Type", "Used", "Chargeable");
                        java.util.List<java.util.List<String>> rows = java.util.List.of(
                                java.util.List.of("Calls", Integer.toString(usedMin), "0"),
                                java.util.List.of("SMS", Integer.toString(usedSms), "0"));
                        System.out.print(DataViewFormatter.table(headers, rows));
                        System.out.println("Status: " + RechargeService.prepaidActiveStatus(customerId));
                    } else {
                        double callCost = chargeMin * plan.callRate;
                        double smsCost = chargeSms * plan.smsRate;
                        double rent = plan.monthlyRent;
                        double usageSubtotal = callCost + smsCost;
                        double estCharge = usageSubtotal + rent;
                        System.out.println();
                        System.out.println("BILLING SUMMARY (" + DataViewFormatter.monthLabel(month) + ") - Plan: " + plan.name);
                        java.util.List<String> headers = java.util.List.of("Type", "Used", "Chargeable", "Rate",
                                "Cost");
                        java.util.List<java.util.List<String>> rows = java.util.List.of(
                                java.util.List.of("Calls", Integer.toString(usedMin), Integer.toString(chargeMin),
                                        DataViewFormatter.money(plan.callRate), DataViewFormatter.money(callCost)),
                                java.util.List.of("SMS", Integer.toString(usedSms), Integer.toString(chargeSms),
                                        DataViewFormatter.money(plan.smsRate), DataViewFormatter.money(smsCost)));
                        System.out.print(DataViewFormatter.table(headers, rows));
                        java.util.List<java.util.List<String>> totals = java.util.List.of(
                                java.util.List.of("Monthly Rent", DataViewFormatter.money(rent)),
                                java.util.List.of("Usage Subtotal", DataViewFormatter.money(usageSubtotal)),
                                java.util.List.of("Estimated Total", DataViewFormatter.money(estCharge)));
                        System.out.print(DataViewFormatter.table(java.util.List.of("Item", "Amount"), totals));
                    }
                }
            }
        }
    }

    // Lightweight public-style helpers used by the menu via CustomerService
    static void printUsageSnapshot(int customerId) {
        String acc = AccountService.getAccountType(customerId);
        CustomerService.PlanOption plan = PlanService.fetchPlanForCustomer(customerId);
        String month = currentBillingMonth();
        if (plan == null) {
            if ("PREPAID".equals(acc)) {
                System.out.println("No active pack. Please recharge a prepaid pack to start using services.");
            } else {
                System.out.println("No plan assigned. Please pick a postpaid plan.");
            }
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            // Ensure a row exists so the summary always shows
            ensureUsageRow(conn, customerId, month);
            showCurrentUsage(conn, customerId, plan, month, acc);
        } catch (SQLException e) {
            System.out.println("Unable to load usage snapshot: " + e.getMessage());
        }
    }

    static void printDetailedUsage(int customerId) {
        // For now, detailed view equals snapshot summary; can be expanded later with history
        printUsageSnapshot(customerId);
    }
}
