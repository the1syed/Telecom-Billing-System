import java.util.Scanner;
import admin.AdminMenu;
import customer.CustomerMenu;
import utils.InputUtil;

public class TelecomBillingSystem {

    public static void main(String[] args) {
    // Ensure database schema exists before any interaction
    SchemaInitializer.initialize();
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("\n╔════════════════════════════════════════╗");
                System.out.println("║     TELECOM BILLING SYSTEM             ║");
                System.out.println("╠════════════════════════════════════════╣");
                System.out.println("║  1. 👤 Administrator Access            ║");
                System.out.println("║  2. 📱 Customer Portal                 ║");
                System.out.println("║  3. 🚪 Exit Application                ║");
                System.out.println("╚════════════════════════════════════════╝");
                int choice;
                try {
                    choice = InputUtil.readInt(sc, "Please select an option (1-3): ", 1, 3);
                } catch (InputUtil.InputExceededException ex) {
                    System.out.println("Too many invalid attempts. Exiting application.");
                    sc.close();
                    System.exit(1);
                    return; // unreachable, but for clarity
                }

                switch (choice) {
                    case 1 -> AdminMenu.showAdminMenu(sc);
                    case 2 -> CustomerMenu.showCustomerMenu();
                    case 3 -> {
                        System.out.println("\n✅ Thank you for using our Telecom Billing System!");
                        System.out.println("👋 Have a great day!");
                        sc.close();
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid choice. Please try again! ");
                }
            } catch (Exception ex) {
                System.out.println("Unexpected error: " + ex.getMessage());
                // continue loop without crashing
            }
        }
    }
}
