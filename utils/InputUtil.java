package utils;

import java.util.Scanner;

/**
 * InputUtil provides safe, validated console input helpers so the
 * application does not crash on invalid user entries.
 */
public class InputUtil {
    // Maximum allowed attempts for any single input prompt
    public static final int MAX_ATTEMPTS = 5;

    // Unchecked exception to signal the caller that input attempts were exceeded
    public static class InputExceededException extends RuntimeException {
        public InputExceededException(String message) { super(message); }
    }
    
    /**
     * Display a prompt to press Enter to continue and wait for user input.
     */
    public static void pressEnterToContinue(Scanner sc) {
        System.out.print("\nPress Enter to continue...");
        sc.nextLine();
    }

    public static int readInt(Scanner sc, String prompt) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (!line.isEmpty()) {
                try {
                    return Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please try again! " + attemptsInfo(attempts));
                }
            } else {
                System.out.println("Input cannot be empty." + attemptsInfo(attempts));
            }
        }
        throw new InputExceededException("Maximum attempts reached for integer input");
    }

    public static int readInt(Scanner sc, String prompt, int min, int max) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (line.isEmpty()) {
                System.out.println("Input cannot be empty." + attemptsInfo(attempts));
                continue;
            }
            try {
                int val = Integer.parseInt(line);
                if (val == 0 && min == 0) {
                    return 0; // Allow 0 for cancel operation when min==0
                }
                if (val < min || val > max) {
                    System.out.println("Value must be between " + min + " and " + max + "." + attemptsInfo(attempts));
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please try again! " + attemptsInfo(attempts));
            }
        }
        throw new InputExceededException("Maximum attempts reached for ranged integer input");
    }

    public static double readDouble(Scanner sc, String prompt, double min) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (line.isEmpty()) {
                System.out.println("Input cannot be empty." + attemptsInfo(attempts));
                continue;
            }
            try {
                double v = Double.parseDouble(line);
                if (v < min) {
                    System.out.println("Value must be greater than or equal to " + min + "." + attemptsInfo(attempts));
                } else {
                    return v;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please try again!" + attemptsInfo(attempts));
            }
        }
        throw new InputExceededException("Maximum attempts reached for decimal input");
    }

    // Read a line that must not be empty unless allowEmpty = true.
    public static String readNonEmptyLine(Scanner sc, String prompt, boolean allowEmpty) {
        if (allowEmpty) { // single read, allow empty result
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine() : "";
            return line.trim();
        }
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine() : "";
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
            System.out.println("Value cannot be empty." + attemptsInfo(attempts));
        }
        throw new InputExceededException("Maximum attempts reached for text input");
    }

    /**
     * Read an optional integer value. If the user presses Enter without typing,
     * returns currentVal unchanged. Otherwise validates [min,max] and retries
     * up to MAX_ATTEMPTS on invalid input.
     */
    public static Integer readOptionalInt(Scanner sc, String prompt, Integer currentVal, int min, int max) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (line.isEmpty()) return currentVal; // keep existing
            try {
                int v = Integer.parseInt(line);
                if (v < min || v > max) {
                    System.out.println("Value must be between " + min + " and " + max + "." + attemptsInfo(attempts));
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again!" + attemptsInfo(attempts));
            }
        }
        throw new InputExceededException("Maximum attempts reached for optional integer input");
    }

    /**
     * Read an optional double value. If the user presses Enter without typing,
     * returns currentVal unchanged. Otherwise validates v >= min and retries up
     * to MAX_ATTEMPTS on invalid input.
     */
    public static Double readOptionalDouble(Scanner sc, String prompt, Double currentVal, double min) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (line.isEmpty()) return currentVal; // keep existing
            try {
                double v = Double.parseDouble(line);
                if (v < min) {
                    System.out.println("Value must be greater than or equal to " + min + "." + attemptsInfo(attempts));
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again!" + attemptsInfo(attempts));
            }
        }
        throw new InputExceededException("Maximum attempts reached for optional decimal input");
    }

    /**
     * Read a phone number consisting ONLY of digits. Default rule here:
     * exactly 10 digits (adjust easily if requirements change). Keeps prompting
     * until user enters a valid string. Prevents accidental alpha input.
     */
    public static String readPhoneNumber(Scanner sc, String prompt) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if (line.matches("\\d{10}")) {
                return line;
            }
            System.out.println("Invalid phone number. Enter exactly 10 digits (0-9) with no spaces or symbols." + attemptsInfo(attempts));
        }
        throw new InputExceededException("Maximum attempts reached for phone number input");
    }

    // Variant that allows entering 0 to cancel. Returns null if cancelled.
    public static String readPhoneNumberOrCancel(Scanner sc, String prompt) {
        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            System.out.print(prompt);
            String line = sc.hasNextLine() ? sc.nextLine().trim() : "";
            if ("0".equals(line)) return null; // cancel
            if (line.matches("\\d{10}")) return line;
            System.out.println("Invalid phone number. Enter exactly 10 digits or 0 to cancel." + attemptsInfo(attempts));
        }
        throw new InputExceededException("Maximum attempts reached for phone number input");
    }

    /**
     * Present a numeric menu (1..N) with optional 0 for cancel and return the selected index.
     * Returns 0 when user selects cancel and allowCancel is true; otherwise returns 1..N.
     */
    public static int readChoice(Scanner sc, String title, String[] options, boolean allowCancel) {
        if (options == null || options.length == 0)
            throw new IllegalArgumentException("options must not be empty");
        System.out.println();
        if (title != null && !title.isEmpty()) {
            System.out.println(title + ":");
        }
        for (int i = 0; i < options.length; i++) {
            System.out.println(" " + (i + 1) + ") " + options[i]);
        }
        if (allowCancel) {
            System.out.println(" 0) Cancel");
        }
        int min = allowCancel ? 0 : 1;
        int max = options.length;
        return readInt(sc, "Select option (" + min + "-" + max + "): ", min, max);
    }

    private static String attemptsInfo(int attempts) {
        return " (attempt " + attempts + "/" + MAX_ATTEMPTS + ")";
    }
}
