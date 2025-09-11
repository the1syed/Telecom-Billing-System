package customer;

import java.util.Scanner;

import utils.DataViewFormatter;
import utils.InputUtil;

public class CustomerMenu {

    public static void showCustomerMenu() {
        Scanner sc = new Scanner(System.in);
        int customerId = loginOrRegister(sc);
        if (customerId == -1)
            return; // back to main

        while (true) {
            // Usage snapshot and standardized menu layout (plain ASCII)
            System.out.println();
            CustomerService.printUsageSnapshot(customerId);
            System.out.println();
            final int width = 66;
            String line = repeat('-', width);
            System.out.println(line);
            System.out.println(center("CUSTOMER SELF-CARE PORTAL", width));
            System.out.println(line);
            System.out.println(center("ACCOUNT", width));
            System.out.println(" 1) View Profile             | account details");
            System.out.println(" 2) Update Profile           | name / address");
            System.out.println(" 3) Delete Account           | permanent");
            System.out.println(line);
            System.out.println(center("PLAN", width));
            System.out.println(" 4) Current Plan Details     | plan benefits & rates");
            // Dynamic menu per account type
            String acct = CustomerService.getAccountTypePublic(customerId);
            if ("PREPAID".equals(acct)) {
                // Prepaid: hide Change Plan; allow only Recharge (multi recharges extend validity)
                System.out.println(" 5) Recharge (Prepaid)       | extend validity (stackable)");
            } else {
                // Postpaid: hide Recharge; offer schedule change options
                System.out.println(" 5) Schedule Plan Upgrade    | takes effect next billing cycle");
                System.out.println(" 6) Schedule Plan Downgrade  | takes effect next cycle; immediate only by upgrade with proration");
            }
            System.out.println(line);
            System.out.println(center("BILLING", width));
            System.out.println(" 7) Billing History          | past generated bills");
            System.out.println(" 8) Generate Month Bill      | postpaid only");
            System.out.println(line);
            System.out.println(center("USAGE & SIMULATION", width));
            System.out.println(" 9) Simulate Call            | add call minutes");
            System.out.println("10) Simulate SMS             | add sms usage");
            System.out.println("11) Detailed Usage Report    | full breakdown");
            System.out.println("12) Fraud Detection          | suspicious activity");
            System.out.println("13) Customer Support         | raise a ticket");
            System.out.println("15) My Tickets               | status & history");
            System.out.println("16) Recharge History         | prepaid recharges");
            System.out.println(line);

            System.out.println(center("SESSION", width));
            System.out.println("14) Logout");
            System.out.println(line);
            int choice;
            try {
                choice = InputUtil.readInt(sc, "Select option (1-16): ", 1, 16);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Logging out.");
                return;
            }

            switch (choice) {
                case 1 -> CustomerService.viewMyDetails(customerId);
                case 2 -> CustomerService.updateProfile(customerId, sc);
                case 3 -> {
                    System.out.println("\nWARNING: This will permanently delete your account and all associated data!");
                    String confirm;
                    try {
                        confirm = InputUtil.readNonEmptyLine(sc, "Type 'CONFIRM' to proceed with account deletion: ",
                                false);
                    } catch (InputUtil.InputExceededException ex) {
                        System.out.println("Too many invalid attempts. Deletion canceled.");
                        break;
                    }
                    if ("CONFIRM".equals(confirm)) {
                        CustomerService.deleteProfile(customerId);
                        System.out.println("Account deleted.");
                        return;
                    } else {
                        System.out.println("Deletion canceled.");
                    }
                }
                case 4 -> CustomerService.viewMyPlan(customerId);
                case 5 -> {
                    if ("PREPAID".equals(CustomerService.getAccountTypePublic(customerId))) {
                        CustomerService.rechargePrepaid(customerId, sc);
                    } else {
                        CustomerService.schedulePlanChange(customerId, sc, true);
                    }
                }
                case 6 -> {
                    if ("PREPAID".equals(CustomerService.getAccountTypePublic(customerId))) {
                        System.out.println("Option not available for PREPAID.");
                    } else {
                        CustomerService.schedulePlanChange(customerId, sc, false);
                    }
                }
                case 7 -> CustomerService.viewMyBills(customerId, sc);
                case 8 -> CustomerService.generateMyBill(customerId);
                case 9 -> CustomerService.simulateCall(customerId, sc);
                case 10 -> CustomerService.simulateSms(customerId, sc);
                case 11 -> CustomerService.printDetailedUsage(customerId);
                case 12 -> CustomerService.reportSpam(customerId);
                case 13 -> CustomerService.raiseSupportTicket(customerId);
                case 15 -> CustomerService.viewMyTickets(customerId);
                case 16 -> CustomerService.viewRechargeHistory(customerId);
                case 14 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static int loginOrRegister(Scanner sc) {
        while (true) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println(" CUSTOMER PORTAL");
            System.out.println("------------------------------------------------------------");
            System.out.println(" 1) Login to Existing Account");
            System.out.println(" 2) Register New Account");
            System.out.println(" 3) Back to Main Menu");
            System.out.println("------------------------------------------------------------");
            int choice;
            try {
                choice = InputUtil.readInt(sc, "Please select an option (1-3): ", 1, 3);
            } catch (InputUtil.InputExceededException ex) {
                System.out.println("Too many invalid attempts. Returning to main menu.");
                return -1;
            }
            switch (choice) {
                case 1 -> {
                    // Enforce numeric 10-digit phone for login (plain formatting)
                    System.out.println("\n--- CUSTOMER LOGIN ---");
                    String phone;
                    try {
                        phone = InputUtil.readPhoneNumber(sc, "Phone Number (10 digits): ");
                    } catch (InputUtil.InputExceededException ex) {
                        System.out.println("Too many invalid attempts. Returning to previous menu.");
                        break;
                    }
                    System.out.println("Locating account...");
                    int id = CustomerService.authenticate(phone);
                    if (id != -1) {
                        String name = CustomerService.getCustomerNameById(id);
                        if (name == null || name.isBlank())
                            name = "Customer";
                        System.out.println("Login successful. Welcome, " + name + "!");
                        return id;
                    }
                    System.out.println("No customer found with that phone number.");
                }
                case 2 -> {
                    System.out.println("\n--- NEW ACCOUNT REGISTRATION ---");
                    System.out.println("Enter required information below:");
                    String name;
                    try {
                        name = InputUtil.readNonEmptyLine(sc, "Full Name: ", false);
                    } catch (InputUtil.InputExceededException ex) {
                        System.out.println("Too many invalid attempts. Returning to previous menu.");
                        break;
                    }
                    String address = InputUtil.readNonEmptyLine(sc, "Address (optional, press Enter to skip): ", true);
                    // Account type selection (numeric)
                    String accountType;
                    {
                        String[] opts = {"PREPAID", "POSTPAID"};
                        int sel;
                        try {
                            sel = InputUtil.readChoice(sc, "Account Type", opts, true);
                        } catch (InputUtil.InputExceededException ex) {
                            System.out.println("Too many invalid attempts. Returning to previous menu.");
                            break;
                        }
                        if (sel == 0) { System.out.println("Registration cancelled."); break; }
                        accountType = opts[sel - 1];
                    }
                    if (accountType == null) break; // cancelled
                    // Plan selection filtered by account type (ids hidden from user)
                    java.util.List<CustomerService.PlanOption> plans = CustomerService.fetchPlansByType(accountType);
                    Integer selectedPlanId = null;
                    if (!plans.isEmpty()) {
                        System.out.println();
                        System.out.println("AVAILABLE " + accountType + " PLANS (enter 0 to skip)");
                        java.util.List<String> headers;
                        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                        if ("PREPAID".equals(accountType)) {
                            headers = java.util.List.of("#", "Name", "ValidityDays", "PackPrice");
                            for (int i = 0; i < plans.size(); i++) {
                                var p = plans.get(i);
                                rows.add(java.util.List.of(
                                        Integer.toString(i + 1),
                                        p.name,
                                        p.validityDays == null ? "-" : Integer.toString(p.validityDays),
                                        p.packPrice == null ? "-" : DataViewFormatter.money(p.packPrice)));
                            }
                        } else {
                            headers = java.util.List.of("#", "Name", "Monthly", "CallRate", "SmsRate");
                            for (int i = 0; i < plans.size(); i++) {
                                var p = plans.get(i);
                                rows.add(java.util.List.of(
                                        Integer.toString(i + 1),
                                        p.name,
                                        String.format("%.2f", p.monthlyRent),
                                        String.format("%.4f", p.callRate),
                                        String.format("%.4f", p.smsRate)));
                            }
                        }
                        System.out.print(DataViewFormatter.table(headers, rows));
                        int sel;
                        try {
                            sel = InputUtil.readInt(sc, "Enter your plan choice (0-" + plans.size() + "): ", 0,
                                    plans.size());
                        } catch (InputUtil.InputExceededException ex) {
                            System.out.println("Too many invalid attempts. Skipping plan selection.");
                            sel = 0;
                        }
                        if (sel >= 1)
                            selectedPlanId = plans.get(sel - 1).id; // internal id retained
                    }
                    System.out.println("Creating account...");
                    // For PREPAID: do not assign the plan at registration time unless payment is completed.
                    // We'll register the account first, and only after successful payment (recharge) will the plan be applied.
                    Integer planIdForRegistration = selectedPlanId;
                    if ("PREPAID".equals(accountType)) {
                        planIdForRegistration = null; // defer plan assignment until after payment
                    }
                    CustomerService.RegistrationResult result = CustomerService.registerAutoWithPlan(name, address,
                            planIdForRegistration, accountType);
                    if (result != null) {
                        System.out.println("Registration successful.");
                        System.out.println();
                        System.out.println("ACCOUNT CREATED");
                        System.out.println("------------------------------------------------------------");
                        System.out.println("Phone Number : " + result.phoneNumber);
                        System.out.println("(Save this number for future logins)");
                        System.out.println("------------------------------------------------------------");
                        // Optional: For PREPAID, offer to recharge immediately
                        if ("PREPAID".equals(accountType)) {
                            if (selectedPlanId != null) {
                                System.out.println("Note: Your selected prepaid plan will be activated only after successful payment.");
                            }
                            String[] yn = {"Yes", "No"};
                            int sel;
                            try {
                                sel = InputUtil.readChoice(sc, "Recharge now?", yn, true);
                            } catch (InputUtil.InputExceededException ex) {
                                sel = 2; // default to No
                            }
                            if (sel == 1 && selectedPlanId != null) {
                                CustomerService.rechargePrepaid(result.customerId, sc, selectedPlanId);
                            } else if (selectedPlanId != null) {
                                System.out.println("No payment made. Plan not assigned. You can recharge later from the menu.");
                            }
                        }
                        // Customer ID intentionally not shown (internal)
                        return result.customerId;
                    }
                    System.out.println("Registration failed. Please try again later.");
                }
                case 3 -> {
                    System.out.println("Returning to main menu...");
                    return -1; // back
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ----------------- Local helpers -----------------
    private static String repeat(char c, int count) {
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }

    private static String center(String text, int width) {
        if (text == null)
            text = "";
        if (text.length() >= width)
            return text;
        int pad = width - text.length();
        int left = pad / 2;
        int right = pad - left;
        return repeat(' ', left) + text + repeat(' ', right);
    }
}
