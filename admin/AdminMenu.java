package admin;

import java.util.Scanner;

import utils.InputUtil;

public class AdminMenu {

    public static void showAdminMenu(Scanner sc) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║       ADMINISTRATOR LOGIN              ║");
        System.out.println("╚════════════════════════════════════════╝");
        String username = null, password = null;
        boolean authed = false;
        for (int attempts = 1; attempts <= InputUtil.MAX_ATTEMPTS; attempts++) {
            try {
                username = InputUtil.readNonEmptyLine(sc, "👤 Username: ", false).toLowerCase();
                password = InputUtil.readNonEmptyLine(sc, "🔑 Password: ", false);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts entering credentials. Returning to main menu.");
                return;
            }
            if ("admin".equals(username) && "admin123".equals(password)) { authed = true; break; }
            System.out.println("\n❌ Authentication Failed! Invalid credentials. (attempt " + attempts + "/" + InputUtil.MAX_ATTEMPTS + ")");
        }
        if (!authed) { System.out.println("Too many invalid attempts. Returning to main menu."); return; }
        System.out.println("\n✅ Login successful! Welcome, Administrator.");
        
        while (true) {
            int custCount = AdminService.countCustomers();
            int planCount = AdminService.countPlans();
            System.out.println();
            System.out.println("================= ADMIN OPERATIONS CENTER =================");
            System.out.println("Accounts: " + custCount + "  |  Plans: " + planCount + "  |  Period: " + java.time.YearMonth.now());
            System.out.println("-----------------------------------------------------------");
            System.out.println("CUSTOMERS");
            System.out.println(" 1. List All Customers");
            System.out.println(" 2. View Customer (by phone)");
            System.out.println(" 3. Delete Customer (by phone)");
            System.out.println("-----------------------------------------------------------");
            System.out.println("PLANS");
            System.out.println(" 4. List All Plans");
            System.out.println(" 5. Add New Plan");
            System.out.println(" 6. Edit Plan");
            System.out.println(" 7. Delete Plan");
            System.out.println(" 8. Most Popular Plan(s)");
            System.out.println(" 9. Least Popular Plan(s)");
            System.out.println("-----------------------------------------------------------");
            System.out.println("BILLING");
            System.out.println("10. All Bills");
            System.out.println("11. Revenue Summary");
            System.out.println("12. Generate Monthly Bills (Postpaid)");
            System.out.println("-----------------------------------------------------------");
            System.out.println("USAGE & ANALYTICS");
            System.out.println("13. Current Month Usage Overview");
            System.out.println("-----------------------------------------------------------");
            System.out.println("SUPPORT");
            System.out.println("14. List Support Tickets");
            System.out.println("15. View Ticket Detail");
            System.out.println("16. Update Ticket (status/priority/assignee)");
            System.out.println("17. Add Ticket Comment");
            System.out.println("-----------------------------------------------------------");
            System.out.println("SESSION");
            System.out.println("18. Logout");
            System.out.println("===========================================================");
            int choice;
            try {
                choice = InputUtil.readInt(sc, "Select option (1-18): ", 1, 18);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Logging out.");
                return;
            }

            switch (choice) {
                case 1 -> AdminService.viewAllCustomers();
                case 2 -> AdminService.viewCustomerByPhone(sc);
                case 3 -> AdminService.deleteCustomer(sc);
                case 4 -> AdminService.viewAllPlans();
                case 5 -> AdminService.addNewPlan(sc);
                case 6 -> AdminService.editPlan(sc);
                case 7 -> AdminService.deletePlan(sc);
                case 8 -> AdminService.mostPopularPlan();
                case 9 -> AdminService.leastPopularPlan();
                case 10 -> AdminService.viewAllBills();
                case 11 -> AdminService.revenueReport();
                case 12 -> AdminService.generateMonthlyBillsForAllPostpaid();
                case 13 -> AdminService.usageOverview();
                case 14 -> AdminService.listSupportTickets(sc);
                case 15 -> AdminService.viewTicketDetail(sc);
                case 16 -> AdminService.updateTicket(sc);
                case 17 -> AdminService.addTicketComment(sc);
                case 18 -> { System.out.println("Logging out..."); return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

}
