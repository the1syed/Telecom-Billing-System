package admin;

import java.util.Scanner;


public class AdminService {

    // Facade methods delegating to split services. Signatures unchanged.
    public static void viewAllCustomers() { AdminCustomerService.viewAllCustomers(); }
    public static void viewCustomerByPhone(Scanner sc) { AdminCustomerService.viewCustomerByPhone(sc); }
    public static void deleteCustomer(Scanner sc) { AdminCustomerService.deleteCustomer(sc); }
    public static int countCustomers() { return AdminCustomerService.countCustomers(); }

    public static void viewAllPlans() { AdminPlanService.viewAllPlans(); }
    public static void addNewPlan(Scanner sc) { AdminPlanService.addNewPlan(sc); }
    public static void editPlan(Scanner sc) { AdminPlanService.editPlan(sc); }
    public static void deletePlan(Scanner sc) { AdminPlanService.deletePlan(sc); }
    public static void mostPopularPlan() { AdminPlanService.mostPopularPlan(); }
    public static void leastPopularPlan() { AdminPlanService.leastPopularPlan(); }
    public static int countPlans() { return AdminPlanService.countPlans(); }

    public static void viewAllBills() { AdminBillingService.viewAllBills(); }
    public static void revenueReport() { AdminBillingService.revenueReport(); }
    public static void generateBill(Scanner sc) { AdminBillingService.generateBill(sc); }
    public static void generateMonthlyBillsForAllPostpaid() { AdminBillingService.generateMonthlyBillsForAllPostpaid(); }

    public static void usageOverview() { AdminUsageService.usageOverview(); }

    public static void listSupportTickets(Scanner sc) { AdminSupportService.listSupportTickets(sc); }
    public static void viewTicketDetail(Scanner sc) { AdminSupportService.viewTicketDetail(sc); }
    public static void updateTicket(Scanner sc) { AdminSupportService.updateTicket(sc); }
    public static void addTicketComment(Scanner sc) { AdminSupportService.addTicketComment(sc); }
}
