package admin;

import java.sql.*;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AdminUsageService {

    static void usageOverview() {
        String month = AdminCommon.currentBillingMonth();
        String sql = "SELECT c.NAME, c.PHONE_NUMBER, p.PLAN_NAME, " +
                "NVL(u.USED_CALL_MINUTES,0) USEDM, NVL(u.CHARGEABLE_CALL_MINUTES,0) CHGM, " +
                "NVL(u.USED_SMS,0) USEDS, NVL(u.CHARGEABLE_SMS,0) CHGS " +
                "FROM CUSTOMERS c " +
                "LEFT JOIN PLANS p ON c.PLAN_ID = p.PLAN_ID " +
                "LEFT JOIN CUSTOMER_USAGE u ON c.CUSTOMER_ID = u.CUSTOMER_ID AND u.BILLING_MONTH = ? " +
                "ORDER BY c.NAME";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    rows.add(java.util.List.of(
                        rs.getString("NAME"),
                        rs.getString("PHONE_NUMBER"),
                        rs.getString("PLAN_NAME") == null ? "(none)" : rs.getString("PLAN_NAME"),
                        Integer.toString(rs.getInt("USEDM")),
                        Integer.toString(rs.getInt("CHGM")),
                        Integer.toString(rs.getInt("USEDS")),
                        Integer.toString(rs.getInt("CHGS"))
                    ));
                }
                System.out.println();
                System.out.println("USAGE OVERVIEW - MONTH " + DataViewFormatter.monthLabel(month));
                System.out.print(DataViewFormatter.table(java.util.List.of(
                    "Name", "Phone", "Plan", "UsedMin", "ChgMin", "UsedSMS", "ChgSMS"), rows));
            }
        } catch (SQLException e) { System.out.println("Error generating usage overview: " + e.getMessage()); }
        java.util.Scanner sc = new java.util.Scanner(System.in);
        InputUtil.pressEnterToContinue(sc);
    }
}
