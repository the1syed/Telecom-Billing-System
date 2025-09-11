package admin;

import java.sql.*;
import java.util.Scanner;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AdminPlanService {

    static void viewAllPlans() {
        Scanner sc = new Scanner(System.in);
        String query = "SELECT PLAN_ID, PLAN_NAME, PLAN_TYPE, MONTHLY_RENT, CALL_RATE, FREE_SMS, FREE_CALL_MINUTES, SMS_RATE, VALIDITY_DAYS, PACK_PRICE FROM PLANS ORDER BY PLAN_ID";
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(java.util.List.of(
                    Integer.toString(rs.getInt("PLAN_ID")),
                    rs.getString("PLAN_NAME"),
                    rs.getString("PLAN_TYPE"),
                    rs.getObject("MONTHLY_RENT") == null ? "-" : DataViewFormatter.money(rs.getDouble("MONTHLY_RENT")),
                    rs.getObject("CALL_RATE") == null ? "-" : DataViewFormatter.money(rs.getDouble("CALL_RATE")),
                    rs.getObject("SMS_RATE") == null ? "-" : DataViewFormatter.money(rs.getDouble("SMS_RATE")),
                    rs.getObject("VALIDITY_DAYS") == null ? "-" : Integer.toString(rs.getInt("VALIDITY_DAYS")),
                    rs.getObject("PACK_PRICE") == null ? "-" : DataViewFormatter.money(rs.getDouble("PACK_PRICE"))
                ));
            }
            System.out.println();
            System.out.println("PLANS");
            System.out.print(DataViewFormatter.table(java.util.List.of(
                "ID", "Name", "Type", "Monthly", "CallRate", "SmsRate", "ValidityDays", "PackPrice"), rows));
        } catch (SQLException e) { System.out.println("Error loading plans: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void addNewPlan(Scanner sc) {
        try (Connection conn = DBConnection.getConnection()) {
            String name = InputUtil.readNonEmptyLine(sc, "Enter Plan Name: ", false);
            String type;
            {
                String[] types = {"PREPAID", "POSTPAID"};
                int sel;
                try { sel = InputUtil.readChoice(sc, "Plan Type", types, false); }
                catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
                type = types[sel - 1];
            }
            int rows;
            if ("POSTPAID".equals(type)) {
                double rent = InputUtil.readDouble(sc, "Enter Monthly Rent: ", 0.0);
                double callRate = InputUtil.readDouble(sc, "Enter Call Rate (per minute): ", 0.0);
                double smsRate = InputUtil.readDouble(sc, "Enter SMS Rate (per SMS): ", 0.0);
                String query = "INSERT INTO PLANS (PLAN_NAME, PLAN_TYPE, MONTHLY_RENT, CALL_RATE, FREE_SMS, FREE_CALL_MINUTES, SMS_RATE) VALUES (?, 'POSTPAID', ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, name);
                    ps.setDouble(2, rent);
                    ps.setDouble(3, callRate);
                    ps.setInt(4, 0);
                    ps.setInt(5, 0);
                    ps.setDouble(6, smsRate);
                    rows = ps.executeUpdate();
                }
            } else {
                int validityDays = InputUtil.readInt(sc, "Enter Validity in Days (e.g., 28/56/84): ", 1, 365);
                double packPrice = InputUtil.readDouble(sc, "Enter Pack Price: ", 0.0);
                String query = "INSERT INTO PLANS (PLAN_NAME, PLAN_TYPE, VALIDITY_DAYS, PACK_PRICE) VALUES (?, 'PREPAID', ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, name);
                    ps.setInt(2, validityDays);
                    ps.setDouble(3, packPrice);
                    rows = ps.executeUpdate();
                }
            }
            if (rows > 0) {
                System.out.println("Plan added successfully!");
            } else {
                System.out.println("Failed to add plan.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void editPlan(Scanner sc) {
        java.util.List<java.util.List<String>> preview = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT PLAN_ID, PLAN_NAME, PLAN_TYPE, MONTHLY_RENT, CALL_RATE, FREE_CALL_MINUTES, FREE_SMS, SMS_RATE, VALIDITY_DAYS, PACK_PRICE FROM PLANS ORDER BY PLAN_ID")) {
            while (rs.next()) {
                preview.add(java.util.List.of(
                    Integer.toString(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getObject(4) == null ? "-" : DataViewFormatter.money(rs.getDouble(4)),
                    rs.getObject(5) == null ? "-" : DataViewFormatter.money(rs.getDouble(5)),
                    rs.getObject(8) == null ? "-" : DataViewFormatter.money(rs.getDouble(8)),
                    rs.getObject(9) == null ? "-" : Integer.toString(rs.getInt(9)),
                    rs.getObject(10) == null ? "-" : DataViewFormatter.money(rs.getDouble(10))
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error loading plans: " + e.getMessage());
        }
        if (!preview.isEmpty()) {
            System.out.println();
            System.out.println("PLANS (for reference)");
            System.out.print(DataViewFormatter.table(java.util.List.of("ID","Name","Type","Monthly","CallRate","SmsRate","Validity","PackPrice"), preview));
        } else {
            System.out.println("No plans available.");
        }
        int planId;
        try {
            planId = InputUtil.readInt(sc, "Enter Plan ID to edit (0 to cancel): ", 0, Integer.MAX_VALUE);
        } catch (InputUtil.InputExceededException ex) {
            System.out.println("Too many invalid attempts. Returning.");
            return;
        }
        if (planId == 0) { System.out.println("Cancelled."); return; }

        String fetch = "SELECT PLAN_NAME, PLAN_TYPE, MONTHLY_RENT, CALL_RATE, FREE_SMS, FREE_CALL_MINUTES, SMS_RATE, VALIDITY_DAYS, PACK_PRICE FROM PLANS WHERE PLAN_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(fetch)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Plan not found."); return; }
                String currName = rs.getString(1);
                String currType = rs.getString(2);
                Double currRent = (Double) rs.getObject(3);
                Double currCallRate = (Double) rs.getObject(4);
                Double currSmsRate = (Double) rs.getObject(7);
                Integer currValidity = (Integer) rs.getObject(8);
                Double currPackPrice = (Double) rs.getObject(9);

                System.out.println("Leave blank to keep current value shown in [brackets].");
                String name = InputUtil.readNonEmptyLine(sc, "Name [" + currName + "]: ", true);
                String typeIn = InputUtil.readNonEmptyLine(sc, "Type [" + currType + "] (PREPAID/POSTPAID): ", true).toUpperCase();
                Double newRent = InputUtil.readOptionalDouble(sc, "Monthly Rent [" + (currRent == null ? "-" : DataViewFormatter.money(currRent)) + "]: ", currRent, 0.0);
                Double newCallRate = InputUtil.readOptionalDouble(sc, "Call Rate [" + (currCallRate == null ? "-" : DataViewFormatter.money(currCallRate)) + "]: ", currCallRate, 0.0);
                Double newSmsRate = InputUtil.readOptionalDouble(sc, "SMS Rate [" + (currSmsRate == null ? "-" : DataViewFormatter.money(currSmsRate)) + "]: ", currSmsRate, 0.0);
                Integer newValidity = InputUtil.readOptionalInt(sc, "Validity Days [" + (currValidity == null ? "-" : currValidity) + "]: ", currValidity, 1, 365);
                Double newPackPrice = InputUtil.readOptionalDouble(sc, "Pack Price [" + (currPackPrice == null ? "-" : DataViewFormatter.money(currPackPrice)) + "]: ", currPackPrice, 0.0);

                String upd = "UPDATE PLANS SET PLAN_NAME = ?, PLAN_TYPE = ?, MONTHLY_RENT = ?, CALL_RATE = ?, FREE_SMS = ?, FREE_CALL_MINUTES = ?, SMS_RATE = ?, VALIDITY_DAYS = ?, PACK_PRICE = ? WHERE PLAN_ID = ?";
                try (PreparedStatement updPs = conn.prepareStatement(upd)) {
                    updPs.setString(1, name.isEmpty() ? currName : name);
                    String newType = typeIn.isEmpty() ? currType : typeIn;
                    if (!newType.equals("PREPAID") && !newType.equals("POSTPAID")) newType = currType;
                    updPs.setString(2, newType);
                    if (newRent == null) updPs.setNull(3, java.sql.Types.NUMERIC); else updPs.setDouble(3, newRent);
                    if (newCallRate == null) updPs.setNull(4, java.sql.Types.NUMERIC); else updPs.setDouble(4, newCallRate);
                    updPs.setInt(5, 0);
                    updPs.setInt(6, 0);
                    if (newSmsRate == null) updPs.setNull(7, java.sql.Types.NUMERIC); else updPs.setDouble(7, newSmsRate);
                    if (newValidity == null) updPs.setNull(8, java.sql.Types.INTEGER); else updPs.setInt(8, newValidity);
                    if (newPackPrice == null) updPs.setNull(9, java.sql.Types.NUMERIC); else updPs.setDouble(9, newPackPrice);
                    updPs.setInt(10, planId);
                    int rows = updPs.executeUpdate();
                    if (rows > 0) System.out.println("Plan updated successfully."); else System.out.println("No changes made.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating plan: " + e.getMessage());
        }
    }

    static void deletePlan(Scanner sc) {
        viewAllPlans();
        int planId;
        try {
            planId = InputUtil.readInt(sc, "Enter Plan ID to delete (0 to cancel): ", 0, Integer.MAX_VALUE);
        } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
        if (planId == 0) { System.out.println("Cancelled."); return; }

        String countSql = "SELECT COUNT(*) FROM CUSTOMERS WHERE PLAN_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement cps = conn.prepareStatement(countSql)) {
            cps.setInt(1, planId);
            int subs = 0; try (ResultSet rs = cps.executeQuery()) { if (rs.next()) subs = rs.getInt(1); }
            if (subs > 0) {
                System.out.println("Cannot delete: " + subs + " customer(s) currently subscribed. Move them to other plans first.");
                return;
            }
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM PLANS WHERE PLAN_ID = ?")) {
                del.setInt(1, planId);
                int rows = del.executeUpdate();
                if (rows > 0) System.out.println("Plan deleted."); else System.out.println("Plan not found.");
            }
        } catch (SQLException e) { System.out.println("Error deleting plan: " + e.getMessage()); }
    }

    static void mostPopularPlan() {
        String sql = "SELECT PLAN_NAME, MONTHLY_RENT, CALL_RATE, SMS_RATE, SUBSCRIBERS FROM (" +
            "SELECT p.PLAN_NAME, p.MONTHLY_RENT, p.CALL_RATE, p.SMS_RATE, " +
                    "COUNT(c.CUSTOMER_ID) AS SUBSCRIBERS, " +
                    "DENSE_RANK() OVER (ORDER BY COUNT(c.CUSTOMER_ID) DESC) AS RK " +
                    "FROM PLANS p LEFT JOIN CUSTOMERS c ON c.PLAN_ID = p.PLAN_ID " +
            "GROUP BY p.PLAN_ID, p.PLAN_NAME, p.MONTHLY_RENT, p.CALL_RATE, p.SMS_RATE" +
                    ") WHERE RK = 1 ORDER BY PLAN_NAME";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(java.util.List.of(
                        rs.getString("PLAN_NAME"),
                        DataViewFormatter.money(rs.getDouble("MONTHLY_RENT")),
                        DataViewFormatter.money(rs.getDouble("CALL_RATE")),
                        DataViewFormatter.money(rs.getDouble("SMS_RATE")),
                        Integer.toString(rs.getInt("SUBSCRIBERS"))
                ));
            }
            System.out.println();
            if (rows.isEmpty()) {
                System.out.println("No plans found.");
            } else {
                System.out.println("MOST POPULAR PLAN(S)");
                System.out.print(DataViewFormatter.table(java.util.List.of(
                    "Name", "Monthly", "CallRate", "SmsRate", "Subscribers"), rows));
            }
        } catch (SQLException e) { System.out.println("Error computing most popular plan: " + e.getMessage()); }
        Scanner sc = new Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }

    static void leastPopularPlan() {
        String sql = "SELECT PLAN_NAME, MONTHLY_RENT, CALL_RATE, SMS_RATE, SUBSCRIBERS FROM (" +
            "SELECT p.PLAN_NAME, p.MONTHLY_RENT, p.CALL_RATE, p.SMS_RATE, " +
                    "COUNT(c.CUSTOMER_ID) AS SUBSCRIBERS, " +
                    "DENSE_RANK() OVER (ORDER BY COUNT(c.CUSTOMER_ID) ASC) AS RK " +
                    "FROM PLANS p LEFT JOIN CUSTOMERS c ON c.PLAN_ID = p.PLAN_ID " +
            "GROUP BY p.PLAN_ID, p.PLAN_NAME, p.MONTHLY_RENT, p.CALL_RATE, p.SMS_RATE" +
                    ") WHERE RK = 1 ORDER BY PLAN_NAME";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(java.util.List.of(
                        rs.getString("PLAN_NAME"),
                        DataViewFormatter.money(rs.getDouble("MONTHLY_RENT")),
                        DataViewFormatter.money(rs.getDouble("CALL_RATE")),
                        DataViewFormatter.money(rs.getDouble("SMS_RATE")),
                        Integer.toString(rs.getInt("SUBSCRIBERS"))
                ));
            }
            System.out.println();
            if (rows.isEmpty()) {
                System.out.println("No plans found.");
            } else {
                System.out.println("LEAST POPULAR PLAN(S)");
                System.out.print(DataViewFormatter.table(java.util.List.of(
                    "Name", "Monthly", "CallRate", "SmsRate", "Subscribers"), rows));
            }
        } catch (SQLException e) { System.out.println("Error computing least popular plan: " + e.getMessage()); }
        Scanner sc = new Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }

    static int countPlans() {
        String sql = "SELECT COUNT(*) FROM PLANS";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }
}
