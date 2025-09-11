package customer;

import java.util.List;
import java.util.Scanner;

// Thin facade: delegates functionality to specialized services.
public class CustomerService {

    // ===== Public API (delegating) =====
    public static void viewMyDetails(int customerId) { AccountService.viewMyDetails(customerId); }

    public static void viewMyPlan(int customerId) {
        // Apply scheduled plan changes effective this month, then show plan
        PlanService.applyScheduledPlanChangeIfDue(customerId);
        PlanService.viewMyPlan(customerId);
    }

    public static void viewMyBills(int customerId, Scanner sc) { BillingService.viewMyBills(customerId, sc); }

    public static void updateProfile(int customerId, Scanner sc) { AccountService.updateProfile(customerId, sc); }

    public static void changePlan(int customerId, Scanner sc) { PlanService.changePlan(customerId, sc); }

    public static String getAccountTypePublic(int customerId) { return AccountService.getAccountType(customerId); }

    public static void schedulePlanChange(int customerId, Scanner sc, boolean isUpgrade) {
        PlanService.schedulePlanChange(customerId, sc, isUpgrade);
    }

    public static void deleteProfile(int customerId) { AccountService.deleteProfile(customerId); }

    public static int authenticate(String phone) { return AuthService.authenticate(phone); }

    public static String getCustomerNameById(int customerId) { return AccountService.getCustomerNameById(customerId); }

    public static void simulateCall(int customerId, Scanner sc) { UsageService.simulateCall(customerId, sc); }

    public static void simulateSms(int customerId, Scanner sc) { UsageService.simulateSms(customerId, sc); }

    public static void generateMyBill(int customerId) { BillingService.generateMyBill(customerId); }

    public static void printUsageSnapshot(int customerId) { UsageService.printUsageSnapshot(customerId); }

    public static void printDetailedUsage(int customerId) { UsageService.printDetailedUsage(customerId); }

    public static void rechargePrepaid(int customerId, Scanner sc) { RechargeService.rechargePrepaid(customerId, sc); }

    public static void rechargePrepaid(int customerId, Scanner sc, Integer preferredPlanId) {
        RechargeService.rechargePrepaid(customerId, sc, preferredPlanId);
    }

    public static void reportSpam(int customerId) { FraudService.reportSpam(customerId); }

    public static void raiseSupportTicket(int customerId) { SupportService.raiseSupportTicket(customerId); }

    public static void viewMyTickets(int customerId) { SupportService.viewMyTickets(customerId); }

    public static void viewRechargeHistory(int customerId) { RechargeService.viewRechargeHistory(customerId); }

    // ===== Package-visible helpers preserved for menus (delegating) =====
    

    static List<PlanOption> fetchPlansByType(String type) { return PlanService.fetchPlansByType(type); }

    

    static RegistrationResult registerAutoWithPlan(String name, String address, Integer planId, String accountType) {
        RegistrationService.RegistrationResult r = RegistrationService.registerAutoWithPlan(name, address, planId, accountType);
        return r == null ? null : new RegistrationResult(r.customerId, r.phoneNumber);
    }

    // ===== Shared DTOs =====
    static class RegistrationResult {
        final int customerId; final String phoneNumber;
        RegistrationResult(int id, String phone) { this.customerId = id; this.phoneNumber = phone; }
    }

    static class PlanOption {
        final int id; final String name; final String planType;
        final double monthlyRent; final double callRate; final int freeSms; final int freeCallMinutes; final double smsRate;
        final Integer validityDays; final Double packPrice;
        PlanOption(int id, String name, String planType, double monthlyRent, double callRate, int freeSms,
                   int freeCallMinutes, double smsRate, Integer validityDays, Double packPrice) {
            this.id = id; this.name = name; this.planType = planType; this.monthlyRent = monthlyRent; this.callRate = callRate;
            this.freeSms = freeSms; this.freeCallMinutes = freeCallMinutes; this.smsRate = smsRate; this.validityDays = validityDays; this.packPrice = packPrice;
        }
    }
}

