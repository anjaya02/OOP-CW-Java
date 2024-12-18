import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Utils {
    public static final List<String> activityLogs = new CopyOnWriteArrayList<>();
    public static final Object consoleLock = new Object(); // To synchronize console output
    private static final String LOG_FILE = Constants.LOG_FILE;

    // Method to add a log entry
    public static void addLog(String log) {
        activityLogs.add(log);
        // Write logs to file
        synchronized (consoleLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(log);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Failed to write to log file: " + e.getMessage());
            }
        }
    }

    // Synchronized print method
    public static void synchronizedPrint(String message) {
        synchronized (consoleLock) {
            System.out.println(message);
        }
    }

    // Synchronized print with formatted messages
    public static void synchronizedPrintFormat(String format, Object... args) {
        synchronized (consoleLock) {
            System.out.printf(format, args);
            System.out.println();
        }
    }
}
