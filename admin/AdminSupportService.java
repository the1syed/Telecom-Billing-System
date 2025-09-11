package admin;

import java.sql.*;
import java.util.Scanner;

import utils.DBConnection;
import utils.DataViewFormatter;
import utils.InputUtil;

class AdminSupportService {

    static void listSupportTickets(Scanner sc) {
        String statusFilter = InputUtil.readNonEmptyLine(sc, "Filter by Status (OPEN/IN_PROGRESS/RESOLVED/CLOSED/ALL) [ALL]: ", true).toUpperCase();
        if (statusFilter.isEmpty()) statusFilter = "ALL";
        String sql = "SELECT t.TICKET_ID, c.NAME, c.PHONE_NUMBER, t.SUBJECT, t.CATEGORY, t.PRIORITY, t.STATUS, t.ASSIGNED_TO, t.CREATED_AT " +
                "FROM SUPPORT_TICKETS t JOIN CUSTOMERS c ON t.CUSTOMER_ID = c.CUSTOMER_ID " +
                (statusFilter.equals("ALL") ? "" : "WHERE t.STATUS = ? ") +
                "ORDER BY t.CREATED_AT DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!statusFilter.equals("ALL")) ps.setString(1, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                while (rs.next()) {
                    rows.add(java.util.List.of(
                        Integer.toString(rs.getInt("TICKET_ID")),
                        rs.getString("NAME"),
                        rs.getString("PHONE_NUMBER"),
                        rs.getString("SUBJECT"),
                        rs.getString("CATEGORY"),
                        rs.getString("PRIORITY"),
                        rs.getString("STATUS"),
                        rs.getString("ASSIGNED_TO") == null ? "-" : rs.getString("ASSIGNED_TO"),
                        rs.getDate("CREATED_AT").toString()
                    ));
                }
                System.out.println();
                System.out.println("SUPPORT TICKETS" + (statusFilter.equals("ALL") ? "" : (" (" + statusFilter + ")")));
                if (rows.isEmpty()) System.out.println("(none)"); else System.out.print(DataViewFormatter.table(java.util.List.of(
                    "ID","Customer","Phone","Subject","Category","Priority","Status","Assignee","Created"), rows));
            }
        } catch (SQLException e) { System.out.println("Error listing tickets: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void viewTicketDetail(Scanner sc) {
        int ticketId;
        try { ticketId = InputUtil.readInt(sc, "Enter Ticket ID: ", 1, Integer.MAX_VALUE); }
        catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
        String headSql = "SELECT t.TICKET_ID, c.NAME, c.PHONE_NUMBER, t.SUBJECT, t.CATEGORY, t.PRIORITY, t.STATUS, t.ASSIGNED_TO, t.CREATED_AT, t.UPDATED_AT, t.ISSUE_DESCRIPTION " +
                "FROM SUPPORT_TICKETS t JOIN CUSTOMERS c ON t.CUSTOMER_ID = c.CUSTOMER_ID WHERE t.TICKET_ID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(headSql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Ticket not found."); return; }
                java.util.List<java.util.List<String>> rows = java.util.List.of(java.util.List.of(
                    Integer.toString(rs.getInt("TICKET_ID")),
                    rs.getString("NAME"),
                    rs.getString("PHONE_NUMBER"),
                    rs.getString("SUBJECT"),
                    rs.getString("CATEGORY"),
                    rs.getString("PRIORITY"),
                    rs.getString("STATUS"),
                    rs.getString("ASSIGNED_TO") == null ? "-" : rs.getString("ASSIGNED_TO"),
                    rs.getDate("CREATED_AT").toString(),
                    rs.getDate("UPDATED_AT").toString()
                ));
                System.out.println();
                System.out.println("TICKET DETAIL");
                System.out.print(DataViewFormatter.table(java.util.List.of("ID","Customer","Phone","Subject","Category","Priority","Status","Assignee","Created","Updated"), rows));
                System.out.println("Description:\n" + rs.getString("ISSUE_DESCRIPTION"));
            }

            String csql = "SELECT AUTHOR_TYPE, COMMENT_TEXT, CREATED_AT FROM TICKET_COMMENTS WHERE TICKET_ID = ? ORDER BY CREATED_AT";
            try (PreparedStatement cps = conn.prepareStatement(csql)) {
                cps.setInt(1, ticketId);
                try (ResultSet crs = cps.executeQuery()) {
                    java.util.List<java.util.List<String>> cRows = new java.util.ArrayList<>();
                    while (crs.next()) {
                        cRows.add(java.util.List.of(
                            crs.getString(1),
                            crs.getString(2),
                            crs.getDate(3).toString()
                        ));
                    }
                    System.out.println("COMMENTS");
                    if (cRows.isEmpty()) System.out.println("(no comments)"); else System.out.print(DataViewFormatter.table(java.util.List.of("Author","Comment","Created"), cRows));
                }
            }
        } catch (SQLException e) { System.out.println("Error retrieving ticket: " + e.getMessage()); }
        InputUtil.pressEnterToContinue(sc);
    }

    static void updateTicket(Scanner sc) {
        int ticketId;
        try { ticketId = InputUtil.readInt(sc, "Enter Ticket ID to update: ", 1, Integer.MAX_VALUE); }
        catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
        String newStatus = InputUtil.readNonEmptyLine(sc, "New Status (OPEN/IN_PROGRESS/RESOLVED/CLOSED) [skip]: ", true).toUpperCase();
        String newPriority = InputUtil.readNonEmptyLine(sc, "New Priority (LOW/MEDIUM/HIGH) [skip]: ", true).toUpperCase();
        String assignee = InputUtil.readNonEmptyLine(sc, "Assign To (email or name) [skip]: ", true);
        StringBuilder sql = new StringBuilder("UPDATE SUPPORT_TICKETS SET ");
        java.util.List<String> sets = new java.util.ArrayList<>();
        if (!newStatus.isEmpty() && newStatus.matches("OPEN|IN_PROGRESS|RESOLVED|CLOSED")) sets.add("STATUS = ?");
        if (!newPriority.isEmpty() && newPriority.matches("LOW|MEDIUM|HIGH")) sets.add("PRIORITY = ?");
        if (!assignee.isEmpty()) sets.add("ASSIGNED_TO = ?");
        sets.add("UPDATED_AT = SYSDATE");
        if (sets.isEmpty()) { System.out.println("Nothing to update."); return; }
        sql.append(String.join(", ", sets));
        sql.append(" WHERE TICKET_ID = ?");
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (sql.indexOf("STATUS = ?") >= 0) { ps.setString(idx++, newStatus); }
            if (sql.indexOf("PRIORITY = ?") >= 0) { ps.setString(idx++, newPriority); }
            if (sql.indexOf("ASSIGNED_TO = ?") >= 0) { ps.setString(idx++, assignee); }
            ps.setInt(idx, ticketId);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Ticket updated."); else System.out.println("Ticket not found.");
        } catch (SQLException e) { System.out.println("Error updating ticket: " + e.getMessage()); }
    }

    static void addTicketComment(Scanner sc) {
        int ticketId;
        try { ticketId = InputUtil.readInt(sc, "Enter Ticket ID to comment on: ", 1, Integer.MAX_VALUE); }
        catch (InputUtil.InputExceededException ex) { System.out.println("Too many invalid attempts."); return; }
        String text = InputUtil.readNonEmptyLine(sc, "Comment: ", false);
        String sql = "INSERT INTO TICKET_COMMENTS (TICKET_ID, AUTHOR_TYPE, COMMENT_TEXT) VALUES (?, 'ADMIN', ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.setString(2, text);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Comment added."); else System.out.println("Failed to add comment.");
        } catch (SQLException e) { System.out.println("Error adding comment: " + e.getMessage()); }
    }
}
