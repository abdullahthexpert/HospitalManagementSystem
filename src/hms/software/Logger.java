package hms.software;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String LOG_FILE = "hms_errors.log";
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String level, String module, String message) {
        String entry = "[" + LocalDateTime.now().format(fmt) + "] [" + level + "] [" + module + "] " + message;
        writeToFile(entry);
        logToDatabase(level, module, message, null);
    }

    public static void log(String level, String module, String message, Exception e) {
        String entry = "[" + LocalDateTime.now().format(fmt) + "] [" + level + "] [" + module + "] " + message;
        writeToFile(entry);
        if (e != null) writeToFile("  >> " + e.toString());
        logToDatabase(level, module, message, e);
    }

    private static void writeToFile(String text) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(text);
        } catch (IOException e) {
            System.err.println("Logger file write failed: " + e.getMessage());
        }
    }

    private static void logToDatabase(String level, String module, String message, Exception e) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO error_log(level, module, message, stack_trace) VALUES(?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, level);
            ps.setString(2, module);
            ps.setString(3, message);
            ps.setString(4, e != null ? e.toString() : null);
            ps.executeUpdate();
        } catch (Exception ex) {
            System.err.println("Logger DB write failed: " + ex.getMessage());
        }
    }
}
