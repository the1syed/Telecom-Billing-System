package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Centralized database connection utility
public class DBConnection {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521:FREE";
    private static final String USER = "system";
    private static final String PASSWORD = "1234";

    static {
        try {
            // Modern driver class
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException primary) {
            try {
                // Legacy fallback name (older ojdbc drivers)
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } catch (ClassNotFoundException fallback) {
                String msg = "Oracle JDBC driver not found. Add ojdbc jar to classpath. " +
                        "Troubleshooting:\n" +
                        "1. Place ojdbcXX.jar in project root (already present: ojdbc17.jar).\n" +
                        "2. If running via command line: javac -cp ojdbc17.jar:. *.java && java -cp ojdbc17.jar:. TelecomBillingSystem\n" +
                        "3. In VS Code, ensure settings.json has java.project.referencedLibraries including ojdbc17.jar.\n" +
                        "4. Confirm jar not corrupted: jar tf ojdbc17.jar (should list classes).";
                throw new RuntimeException(msg, fallback);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
