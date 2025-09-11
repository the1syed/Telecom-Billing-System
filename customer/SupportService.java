package customer;

import java.sql.*;
import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class SupportService {

    static void raiseSupportTicket(int customerId) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        System.out.println("\n🛠 Customer Support - Raise a Ticket");
        String subject = InputUtil.readNonEmptyLine(sc, "Subject: ", false);
        String category; {
            String[] cats = {"BILLING", "NETWORK", "PLAN", "ACCOUNT", "GENERAL"};
            int sel; try { sel = InputUtil.readChoice(sc, "Category", cats, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            category = cats[sel - 1];
        }
        String priority; {
            String[] pr = {"LOW", "MEDIUM", "HIGH"};
            int sel; try { sel = InputUtil.readChoice(sc, "Priority", pr, false); } catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
            priority = pr[sel - 1];
        }
        String issue = InputUtil.readNonEmptyLine(sc, "Describe your issue: ", false);

        String insertQuery = "INSERT INTO SUPPORT_TICKETS (CUSTOMER_ID, SUBJECT, CATEGORY, ISSUE_DESCRIPTION, PRIORITY) VALUES (?, ?, ?, ?, ?)";
        String fetchIdQuery = "SELECT TICKET_ID FROM SUPPORT_TICKETS WHERE CUSTOMER_ID = ? ORDER BY CREATED_AT DESC FETCH FIRST 1 ROWS ONLY";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(insertQuery)) {
            ps.setInt(1, customerId); ps.setString(2, subject); ps.setString(3, category); ps.setString(4, issue); ps.setString(5, priority); ps.executeUpdate();
            try (PreparedStatement ps2 = conn.prepareStatement(fetchIdQuery)) {
                ps2.setInt(1, customerId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        int ticketId = rs.getInt("TICKET_ID");
                        System.out.println("🎫 Ticket Raised Successfully!");
                        System.out.println("Your Ticket ID: " + ticketId);
                        System.out.println("Status: OPEN | Priority: " + priority + " | Category: " + category);
                    }
                }
            }
        } catch (SQLException e) { System.out.println("Error while raising support ticket: " + e.getMessage()); }
    }

    static void viewMyTickets(int customerId) {
        String sql = "SELECT TICKET_ID, SUBJECT, CATEGORY, PRIORITY, STATUS, CREATED_AT FROM SUPPORT_TICKETS WHERE CUSTOMER_ID = ? ORDER BY CREATED_AT DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    rows.add(java.util.List.of(
                        Integer.toString(rs.getInt("TICKET_ID")), rs.getString("SUBJECT"), rs.getString("CATEGORY"), rs.getString("PRIORITY"), rs.getString("STATUS"), rs.getDate("CREATED_AT").toString()
                    ));
                }
                System.out.println();
                if (rows.isEmpty()) { System.out.println("You have no support tickets."); }
                else { System.out.print(DataViewFormatter.table(java.util.List.of("TicketId","Subject","Category","Priority","Status","Created"), rows)); }
            }
        } catch (SQLException e) { System.out.println("Error loading your tickets: " + e.getMessage()); }
    }
}
